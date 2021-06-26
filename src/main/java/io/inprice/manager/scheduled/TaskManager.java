package io.inprice.manager.scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.info.TimePeriod;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.scheduled.modifier.AccessLoggerFlusher;
import io.inprice.manager.scheduled.modifier.FreeAccountStopper;
import io.inprice.manager.scheduled.modifier.MemberRemover;
import io.inprice.manager.scheduled.modifier.SubscribedAccountStopper;
import io.inprice.manager.scheduled.notifier.AlarmNotifier;
import io.inprice.manager.scheduled.notifier.FreeAccountsExpirationReminder;
import io.inprice.manager.scheduled.publisher.ActiveLinksPublisher;
import io.inprice.manager.scheduled.publisher.FailedLinksPublisher;
import io.inprice.manager.scheduled.publisher.NewlyAddedLinksPublisher;

public class TaskManager {

  private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;
  private static List<TaskDef> taskDefs;

  public static void start() {
    log.info("TaskManager is starting...");

    taskDefs = new ArrayList<>();
    loadUpdaters();
    loadLinkPublishers();
    loadNotifiers();

    scheduler = Executors.newScheduledThreadPool(taskDefs.size());
    for (TaskDef td: taskDefs) {
    	scheduler.scheduleAtFixedRate(td.getTask(), td.getDelay(), td.getTimePeriod().getInterval(), td.getTimePeriod().getTimeUnit());
    }

    log.info("TaskManager is started with {} workers.", taskDefs.size());
  }

  public static void stop() {
    try {
      scheduler.shutdown();
    } catch (SecurityException e) {
      log.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

  private static void loadUpdaters() {
    taskDefs.add(
  		TaskDef.builder()
  			.task(new NewlyAddedLinksPublisher())
  			.delay(0)
  			.timePeriod(new TimePeriod(1, TimeUnit.MINUTES))
			.build()
		);

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
  			.timePeriod(DateUtils.parseTimePeriod(Props.INTERVAL_STOPPING_FREE_ACCOUNTS))
			.build()
		);

    taskDefs.add(
  		TaskDef.builder()
  			.task(new SubscribedAccountStopper())
  			.delay(0)
  			.timePeriod(DateUtils.parseTimePeriod(Props.INTERVAL_STOPPING_SUBSCRIBED_ACCOUNTS))
			.build()
		);

    taskDefs.add(
  		TaskDef.builder()
  			.task(new AccessLoggerFlusher())
  			.delay(1)
  			.timePeriod(DateUtils.parseTimePeriod(Props.INTERVAL_FLUSHING_ACCESS_LOG_QUEUE))
			.build()
		);
  }
  
  private static void loadLinkPublishers() {
    TimeUnit timeUnit = (SysProps.APP_ENV.equals(AppEnv.PROD) ? TimeUnit.HOURS : TimeUnit.MINUTES);
    String tuName = timeUnit.name().toLowerCase().substring(0, timeUnit.name().length()-1);

    //-----------------------------
    // ACTIVE BUT TRYING LINKS
    //-----------------------------
    //first time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new ActiveLinksPublisher(1, 1, tuName))
  			.delay(1)
  			.timePeriod(new TimePeriod(1, timeUnit))
			.build()
		);

    //second time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new ActiveLinksPublisher(2, 3, tuName))
  			.delay(1)
  			.timePeriod(new TimePeriod(3, timeUnit))
			.build()
		);

    //and last time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new ActiveLinksPublisher(3, 6, tuName))
  			.delay(1)
  			.timePeriod(new TimePeriod(6, timeUnit))
			.build()
		);


    //-----------------------------
    // FAILED BUT TRYING LINKS
    //-----------------------------
    //first time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new FailedLinksPublisher(1, 2, tuName))
  			.delay(1)
  			.timePeriod(new TimePeriod(2, timeUnit))
			.build()
		);

    //second time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new FailedLinksPublisher(2, 7, tuName))
  			.delay(1)
  			.timePeriod(new TimePeriod(7, timeUnit))
			.build()
		);

    //and last time links
    taskDefs.add(
  		TaskDef.builder()
  			.task(new ActiveLinksPublisher(3, 11, tuName))
  			.delay(1)
  			.timePeriod(new TimePeriod(11, timeUnit))
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
  			.timePeriod(DateUtils.parseTimePeriod(Props.INTERVAL_REMINDER_FOR_FREE_ACCOUNTS))
			.build()
		);
  }

}
