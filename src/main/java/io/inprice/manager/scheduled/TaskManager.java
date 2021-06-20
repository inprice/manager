package io.inprice.manager.scheduled;

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
import io.inprice.manager.scheduled.modifier.ReminderForFreeAccounts;
import io.inprice.manager.scheduled.modifier.SubscribedAccountStopper;
import io.inprice.manager.scheduled.publisher.ActiveLinksPublisher;
import io.inprice.manager.scheduled.publisher.FailedLinksPublisher;
import io.inprice.manager.scheduled.publisher.NewlyAddedLinksPublisher;

public class TaskManager {

  private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;

  public static void start() {
    log.info("TaskManager is starting...");

    int[] intervals = {1, 3, 6}; //hour peridos for failing links, see below
    
    int poolSize = 6 + //updaters
    							 intervals.length*2; //activelinks and failedlinks publishers

    scheduler = Executors.newScheduledThreadPool(poolSize);
    
    //updaters
    loadTask(new MemberRemover(), 0, new TimePeriod(3, TimeUnit.HOURS));
    loadTask(new NewlyAddedLinksPublisher(), 0, new TimePeriod(1, TimeUnit.MINUTES));
    loadTask(new FreeAccountStopper(), 0, DateUtils.parseTimePeriod(Props.INTERVAL_STOPPING_FREE_ACCOUNTS));
    loadTask(new SubscribedAccountStopper(), 0, DateUtils.parseTimePeriod(Props.INTERVAL_STOPPING_SUBSCRIBED_ACCOUNTS));
    loadTask(new ReminderForFreeAccounts(), 1, DateUtils.parseTimePeriod(Props.INTERVAL_REMINDER_FOR_FREE_ACCOUNTS));
    loadTask(new AccessLoggerFlusher(), 1, DateUtils.parseTimePeriod(Props.INTERVAL_FLUSHING_ACCESS_LOG_QUEUE));
    //TODO: after subscription done
    //loadTask(new PendingCheckoutsCloser(), 0, DateUtils.parseTimePeriod(Props.TIME_PERIOD_OF_EXPIRING_PENDING_CHECKOUTS()));
    
    TimeUnit timeUnit = (SysProps.APP_ENV.equals(AppEnv.PROD) ? TimeUnit.HOURS : TimeUnit.MINUTES);
    String tuName = timeUnit.name().toLowerCase().substring(0, timeUnit.name().length()-1);
    
    for (int i = 1; i <= intervals.length; i++) {
    	loadTask(new ActiveLinksPublisher(i, intervals[i-1], tuName), 1, new TimePeriod(intervals[i-1], timeUnit));
    }

    for (int i = 1; i <= intervals.length; i++) {
    	loadTask(new FailedLinksPublisher(i, intervals[i-1]*2, tuName), 1, new TimePeriod(intervals[i-1]*2, timeUnit));
		}
    
    log.info("TaskManager is started with {} workers.", poolSize);
  }

  private static void loadTask(Runnable task, int delay, TimePeriod timePeriod) {
    scheduler.scheduleAtFixedRate(task, delay, timePeriod.getInterval(), timePeriod.getTimeUnit());
  }

  public static void stop() {
    try {
      scheduler.shutdown();
    } catch (SecurityException e) {
      log.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

}
