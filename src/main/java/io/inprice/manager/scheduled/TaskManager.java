package io.inprice.manager.scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;

import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.TimePeriod;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.scheduled.modifier.FreeAccountStopper;
import io.inprice.manager.scheduled.modifier.MemberRemover;
import io.inprice.manager.scheduled.modifier.SubscribedAccountStopper;
import io.inprice.manager.scheduled.notifier.AlarmNotifier;
import io.inprice.manager.scheduled.notifier.FreeAccountsExpirationReminder;
import io.inprice.manager.scheduled.publisher.ActiveLinksPublisher;
import io.inprice.manager.scheduled.publisher.FailedLinksPublisher;
import io.inprice.manager.scheduled.publisher.NewlyAddedLinksPublisher;

public class TaskManager {

  private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;
  private static List<TaskDef> taskDefs;

  public static void start() {
    logger.info("TaskManager is starting...");

    taskDefs = new ArrayList<>();
    loadUpdaters();
    loadLinkPublishers();
    loadNotifiers();

    scheduler = Executors.newScheduledThreadPool(taskDefs.size());
    for (TaskDef td: taskDefs) {
    	scheduler.scheduleAtFixedRate(td.getTask(), td.getDelay(), td.getTimePeriod().getInterval(), td.getTimePeriod().getTimeUnit());
    }

    logger.info("TaskManager is started with {} workers.", taskDefs.size());
  }

  public static void stop() {
    try {
      scheduler.shutdown();
    } catch (SecurityException e) {
      logger.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

  private static void loadUpdaters() {
    taskDefs.add(
  		TaskDef.builder()
  			.task(new MemberRemover())
  			.delay(0)
  			.timePeriod(new TimePeriod(3, TimeUnit.HOURS))
			.build()
		);

    taskDefs.add(
  		TaskDef.builder()
  			.task(new FreeAccountStopper())
  			.delay(0)
  			.timePeriod(DateUtils.parseTimePeriod(Props.getConfig().INTERVALS.STOPPING_FREE_ACCOUNTS))
			.build()
		);

    taskDefs.add(
  		TaskDef.builder()
  			.task(new SubscribedAccountStopper())
  			.delay(0)
  			.timePeriod(DateUtils.parseTimePeriod(Props.getConfig().INTERVALS.STOPPING_SUBSCRIBED_ACCOUNTS))
			.build()
		);
  }
  
  private static void loadLinkPublishers() {
  	Connection conn = RabbitMQ.createConnection("manager-link-publisher");

    //-----------------------------
    // NEWLY ADDED LINKS
    //-----------------------------
    taskDefs.add(
  		TaskDef.builder()
  			.task(new NewlyAddedLinksPublisher(conn))
  			.delay(0)
  			.timePeriod(new TimePeriod(1, TimeUnit.MINUTES))
			.build()
		);

    //-----------------------------
    // ACTIVE LINKS
    //-----------------------------
    //first time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new ActiveLinksPublisher(1, 1, conn))
  			.delay(1)
  			.timePeriod(new TimePeriod(1, TimeUnit.HOURS))
			.build()
		);

    //second time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new ActiveLinksPublisher(2, 3, conn))
  			.delay(1)
  			.timePeriod(new TimePeriod(3, TimeUnit.HOURS))
			.build()
		);

    //and last time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new ActiveLinksPublisher(3, 6, conn))
  			.delay(1)
  			.timePeriod(new TimePeriod(6, TimeUnit.HOURS))
			.build()
		);

    //-----------------------------
    // FAILED BUT TRYING LINKS
    //-----------------------------
    //first time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new FailedLinksPublisher(1, 2, conn))
  			.delay(1)
  			.timePeriod(new TimePeriod(2, TimeUnit.HOURS))
			.build()
		);

    //second time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new FailedLinksPublisher(2, 7, conn))
  			.delay(1)
  			.timePeriod(new TimePeriod(7, TimeUnit.HOURS))
			.build()
		);

    //and last time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new ActiveLinksPublisher(3, 11, conn))
  			.delay(1)
  			.timePeriod(new TimePeriod(11, TimeUnit.HOURS))
			.build()
		);
  }

  
  private static void loadNotifiers() {
    taskDefs.add(
  		TaskDef.builder()
  			.task(new AlarmNotifier())
  			.delay(1)
  			.timePeriod(new TimePeriod(5, TimeUnit.MINUTES))
			.build()
		);

    taskDefs.add(
  		TaskDef.builder()
  			.task(new FreeAccountsExpirationReminder())
  			.delay(1)
  			.timePeriod(DateUtils.parseTimePeriod(Props.getConfig().INTERVALS.REMINDER_FOR_FREE_ACCOUNTS))
			.build()
		);
  }

}
