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
    
    int poolSize = 2 + //memberremover + importedlinksremover + one eager activelinkpublisher
    							 intervals.length*2; //activelinks and failedlinks publishers

    scheduler = Executors.newScheduledThreadPool(poolSize);
    
    //updaters must be started immediately
    scheduler.scheduleAtFixedRate(new MemberRemover(), 0, 3, TimeUnit.HOURS);
    scheduler.scheduleAtFixedRate(new PublisherAddedLinks(), 0, 1, TimeUnit.MINUTES); //eager one
    
    TimeUnit timeUnit = (SysProps.APP_ENV.equals(AppEnv.PROD) ? TimeUnit.HOURS : TimeUnit.MINUTES);
    String tuName = timeUnit.name().toLowerCase().substring(0, timeUnit.name().length()-1);
    
    //ACTIVE links which have faced a problem during scraping should be handled in incremental periods
    for (int i = 1; i <= intervals.length; i++) {
    	scheduler.scheduleAtFixedRate(new PublisherActiveLinks(i, intervals[i-1], tuName), 1, intervals[i-1], timeUnit);
    }

    //FAILING links should be handled in incremental periods, too
    for (int i = 1; i <= intervals.length; i++) {
    	scheduler.scheduleAtFixedRate(new PublisherFailedLinks(i, intervals[i-1]*2, tuName), 1, intervals[i-1]*2, timeUnit);
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
