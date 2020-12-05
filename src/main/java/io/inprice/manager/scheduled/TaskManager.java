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

    //all the links other than passive status are suitable candidates
    for (LinkStatus status: LinkStatus.values()) {
      if (! LinkStatus.PASSIVE_GROUP.equals(status.getGroup())) {
        corePoolSize++;
      }
    }

    scheduler = Executors.newScheduledThreadPool(corePoolSize);

    //updaters must be started immediately
    loadTask(new MemberRemover(), 0, DateUtils.parseTimePeriod(Props.TIME_PERIOD_OF_REMOVING_MEMBERS()));
    loadTask(new CompanyStopper(), 0, DateUtils.parseTimePeriod(Props.TIME_PERIOD_OF_STOPPING_COMPANIES()));
    loadTask(new ImportedLinksRemover(), 0, DateUtils.parseTimePeriod(Props.TIME_PERIOD_OF_DELETING_IMPORTED_LINKS()));

    //publishing links
    //in order to giving an opportunity for LinkInactivater, collectors start after some time later
    for (LinkStatus status: LinkStatus.values()) {
      if (! LinkStatus.PASSIVE_GROUP.equals(status.getGroup())) {
        loadTask(new LinkPublisher(status), 1, DateUtils.parseTimePeriod(Props.COLLECTING_PERIOD_OF(status)));
      }
    }

    log.info("TaskManager is started with {} workers.", corePoolSize);
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
