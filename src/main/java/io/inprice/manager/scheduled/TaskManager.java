package io.inprice.manager.scheduled;

import io.inprice.common.info.TimePeriod;
import io.inprice.common.meta.LinkStatus;
import io.inprice.manager.config.Props;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaskManager {

  private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;

  public static void start() {
    log.info("TaskManager is starting up...");

    scheduler = Executors.newScheduledThreadPool(11);

    //updaters
    loadTask(new MemberRemover());

    //standard links
    loadTask(new LinkPublisher(LinkStatus.TOBE_CLASSIFIED, Props.TIMING_FOR_TOBE_CLASSIFIED_LINKS()));
    loadTask(new LinkPublisher(LinkStatus.CLASSIFIED, Props.TIMING_FOR_CLASSIFIED_LINKS()));
    loadTask(new LinkPublisher(LinkStatus.IMPLEMENTED, Props.TIMING_FOR_IMPLEMENTED_LINKS()));
    loadTask(new LinkPublisher(LinkStatus.AVAILABLE, Props.TIMING_FOR_AVAILABLE_LINKS()));
    loadTask(new LinkPublisher(LinkStatus.TOBE_RENEWED, Props.TIMING_FOR_TOBE_RENEWED_LINKS()));
    loadTask(new LinkPublisher(LinkStatus.RESUMED, Props.TIMING_FOR_RESUMED_LINKS()));

    //failed links
    loadTask(new LinkPublisher(LinkStatus.NO_DATA, Props.TIMING_FOR_NO_DATA_ERRORS(), Props.RETRY_LIMIT_FOR_FAILED_LINKS_G1()));
    loadTask(new LinkPublisher(LinkStatus.NOT_AVAILABLE, Props.TIMING_FOR_NOT_AVAILABLE_LINKS(), Props.RETRY_LIMIT_FOR_FAILED_LINKS_G3()));
    loadTask(new LinkPublisher(LinkStatus.SOCKET_ERROR, Props.TIMING_FOR_SOCKET_ERRORS(), Props.RETRY_LIMIT_FOR_FAILED_LINKS_G3()));
    loadTask(new LinkPublisher(LinkStatus.NETWORK_ERROR, Props.TIMING_FOR_NETWORK_ERRORS(), Props.RETRY_LIMIT_FOR_FAILED_LINKS_G3()));

    log.info("TaskManager is started.");
  }

  private static void loadTask(Task task) {
    TimePeriod tp = task.getTimePeriod();
    scheduler.scheduleAtFixedRate(task, 0, tp.getInterval(), tp.getTimeUnit());
  }

  public static void stop() {
    try {
      scheduler.shutdown();
    } catch (SecurityException e) {
      log.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

}
