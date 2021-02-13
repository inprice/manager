package io.inprice.manager.scheduled;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.meta.AppEnv;

public class TaskManager {

  private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;

  public static void start() {
    log.info("TaskManager is starting...");

    int[] intervals = {1, 3, 6}; //hour peridos for failing links, see below
    
    int poolSize = 3 + //memberremover + importedlinksremover + one eager activelinkpublisher
    							 intervals.length*2; //activelinks and failedlinks publishers

    scheduler = Executors.newScheduledThreadPool(poolSize);
    
    //updaters must be started immediately
    scheduler.scheduleAtFixedRate(new MemberRemover(), 0, 3, TimeUnit.HOURS);
    scheduler.scheduleAtFixedRate(new ImportedLinksRemover(), 0, 1, TimeUnit.HOURS);

    //links which are newly added as ACTIVE should be handled in one minute
    scheduler.scheduleAtFixedRate(new LinkPublisher(true, 0), 1, 1, TimeUnit.MINUTES); //eager one
    
    TimeUnit timeUnit = (SysProps.APP_ENV().equals(AppEnv.PROD) ? TimeUnit.HOURS : TimeUnit.MINUTES);
    
    //ACTIVE links which have faced a problem during scraping should be handled in incremental periods
    for (int i = 1; i <= intervals.length; i++) {
    	scheduler.scheduleAtFixedRate(new LinkPublisher(true, i), 1, intervals[i-1], timeUnit);
    }

    //FAILING links should be handled in incremental periods, too
    for (int i = 1; i <= intervals.length; i++) {
    	scheduler.scheduleAtFixedRate(new LinkPublisher(false, i), 1, intervals[i-1]*2, timeUnit);
		}

    log.info("TaskManager is started with {} workers.", poolSize);
  }

  public static void stop() {
    try {
      scheduler.shutdown();
    } catch (SecurityException e) {
      log.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

}
