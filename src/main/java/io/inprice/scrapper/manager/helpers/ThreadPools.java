package io.inprice.scrapper.manager.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.config.SysProps;

public class ThreadPools {

  private static final Logger log = LoggerFactory.getLogger(ThreadPools.class);

  public static final ExecutorService PRICE_CHANGE_POOL;
  public static final ExecutorService DELETED_LINKS_POOL;
  public static final ExecutorService STATUS_CHANGE_POOL;
  public static final ExecutorService AVAILABLE_LINKS_POOL;

  private static final List<ExecutorService> registry;

  static {
    STATUS_CHANGE_POOL = Executors.newFixedThreadPool(2);
    AVAILABLE_LINKS_POOL = Executors.newFixedThreadPool(2);
    PRICE_CHANGE_POOL = Executors.newFixedThreadPool(1);
    DELETED_LINKS_POOL = Executors.newFixedThreadPool(1);

    registry = new ArrayList<>();
    registry.add(STATUS_CHANGE_POOL);
    registry.add(AVAILABLE_LINKS_POOL);
    registry.add(PRICE_CHANGE_POOL);
    registry.add(DELETED_LINKS_POOL);
  }

  public static void shutdown() {
    for (ExecutorService pool : registry) {
      try {
        pool.shutdown();
        pool.awaitTermination(SysProps.WAITING_TIME_FOR_TERMINATION(), TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        log.error("Thread pool termination is interrupted.", e);
      }
    }
  }

}
