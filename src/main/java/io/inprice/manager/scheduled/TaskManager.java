package io.inprice.manager.scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    scheduler = Executors.newScheduledThreadPool(taskList.size());
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
  	Connection activeLinksConn = RabbitMQ.createConnection("manager:active-publisher");
  	Connection failedLinksConn = RabbitMQ.createConnection("manager:failed-publisher");

    taskList.add(new NewlyAddedLinksPublisher(activeLinksConn));

    List<ScheduleDef> activeLinkPublishers = Props.getConfig().SCHEDULES.ACTIVE_LINK_PUBLISHERS;
    for (ScheduleDef alp: activeLinkPublishers) {
    	taskList.add(new ActiveLinksPublisher(alp, activeLinksConn));
    }

    List<ScheduleDef> failedLinkPublishers = Props.getConfig().SCHEDULES.FAILED_LINK_PUBLISHERS;
    for (ScheduleDef flp: failedLinkPublishers) {
    	taskList.add(new FailedLinksPublisher(flp, failedLinksConn));
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
