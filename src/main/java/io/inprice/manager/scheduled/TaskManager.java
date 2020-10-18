package io.inprice.manager.scheduled;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.info.TimePeriod;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;

public class TaskManager {

  private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;

  public static void start() {
    log.info("TaskManager is starting...");

    //two is the number of updaters below
    int corePoolSize = 2;

    //all the links other than passive are available
    for (LinkStatus status: LinkStatus.values()) {
      if (!LinkStatus.PASSIVE_GROUP.equals(status.getGroup())) {
        corePoolSize++;
      }
    }

    scheduler = Executors.newScheduledThreadPool(corePoolSize);

    //publishing links
    for (LinkStatus status: LinkStatus.values()) {
      if (!LinkStatus.PASSIVE_GROUP.equals(status.getGroup())) {
        loadTask(new LinkPublisher(status), DateUtils.parseTimePeriod(Props.TIME_PERIOD_OF(status)));
      }
    }

    //updaters
    loadTask(new MemberRemover(), DateUtils.parseTimePeriod(Props.TIME_PERIOD_OF_REMOVING_MEMBERS()));
    loadTask(new LinkInactivater(), DateUtils.parseTimePeriod(Props.TIME_PERIOD_OF_INACTIVATING_LINKS()));

    log.info("TaskManager is started with {} workers.", corePoolSize);
  }

  private static void loadTask(Runnable task, TimePeriod timePeriod) {
    scheduler.scheduleAtFixedRate(task, 0, timePeriod.getInterval(), timePeriod.getTimeUnit());
  }

  public static void stop() {
    try {
      scheduler.shutdown();
    } catch (SecurityException e) {
      log.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

}
