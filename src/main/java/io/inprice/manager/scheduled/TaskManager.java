package io.inprice.manager.scheduled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import io.inprice.common.config.ScheduleDef;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.manager.config.Props;
import io.inprice.manager.scheduled.modifier.DeletedMemberRemover;
import io.inprice.manager.scheduled.modifier.ExpiredFreeAccountStopper;
import io.inprice.manager.scheduled.modifier.ExpiredSubscriptionStopper;
import io.inprice.manager.scheduled.modifier.PendingCheckoutCloser;
import io.inprice.manager.scheduled.notifier.AlarmNotifier;
import io.inprice.manager.scheduled.notifier.FreeAccountExpirationReminder;
import io.inprice.manager.scheduled.publisher.ActiveLinksPublisher;
import io.inprice.manager.scheduled.publisher.FailedLinksPublisher;
import io.inprice.manager.scheduled.publisher.NewlyAddedLinksPublisher;

public class TaskManager {

  private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;
  private static List<Task> taskList;

  private static volatile Set<String> runningTasksSet = ConcurrentHashMap.newKeySet();
  
  public static void start() {
    logger.info("TaskManager is starting...");

    taskList = new ArrayList<>();
    loadModifiers();
    loadNotifiers();
    loadPublishers();

    int taskCount = 0;
    for (Task task: taskList) {
    	if (task.getSchedule().ACTIVE) taskCount++;
    }

    scheduler = Executors.newScheduledThreadPool(taskCount);

    for (Task task: taskList) {
    	ScheduleDef schedule = task.getSchedule();
    	if (schedule.ACTIVE) {
    		scheduler.scheduleAtFixedRate(task, schedule.DELAY, schedule.EVERY, TimeUnit.valueOf(schedule.PERIOD));
    	}
    }

    logger.info("TaskManager is started with {} workers.", taskList.size());
  }

  private static void loadModifiers() {
    taskList.add(new DeletedMemberRemover());
    taskList.add(new ExpiredFreeAccountStopper());
    taskList.add(new ExpiredSubscriptionStopper());
    taskList.add(new PendingCheckoutCloser());
  }

  private static void loadNotifiers() {
    taskList.add(new AlarmNotifier());
    taskList.add(new FreeAccountExpirationReminder());
  }

  private static void loadPublishers() {
  	try {
	  	//active links
	  	Connection activeLinksConn = RabbitMQ.createConnection("MAN-PUB: active-publisher");
	  	Channel scrappingActiveLinksChannel = activeLinksConn.createChannel();
	  	Channel statusChangingActiveLinksChannel = activeLinksConn.createChannel();

	  	//failed links
	  	Connection failedLinksConn = RabbitMQ.createConnection("MAN-PUB: failed-publisher");
	  	Channel scrappingFailedLinksChannel = failedLinksConn.createChannel();

	    taskList.add(new NewlyAddedLinksPublisher(scrappingActiveLinksChannel, statusChangingActiveLinksChannel));
	
	    List<ScheduleDef> activeLinkPublishers = Props.getConfig().SCHEDULES.ACTIVE_LINK_PUBLISHERS;
	    for (ScheduleDef alp: activeLinkPublishers) {
	    	taskList.add(new ActiveLinksPublisher(alp, scrappingActiveLinksChannel, statusChangingActiveLinksChannel));
	    }
	
	    List<ScheduleDef> failedLinkPublishers = Props.getConfig().SCHEDULES.FAILED_LINK_PUBLISHERS;
	    for (ScheduleDef flp: failedLinkPublishers) {
	    	taskList.add(new FailedLinksPublisher(flp, scrappingFailedLinksChannel, null));
	    }
  	} catch (IOException e) {
  		
  	}
  }

  public static void startTask(String name) {
    runningTasksSet.add(name);
  }

  public static void stopTask(String name) {
    runningTasksSet.remove(name);
  }

  public static boolean isTaskRunning(String name) {
    return runningTasksSet.contains(name);
  }

  public static void stop() {
    try {
      if (scheduler != null) scheduler.shutdown();
    } catch (SecurityException e) {
      logger.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

}
