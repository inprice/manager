package io.inprice.manager.consumer;

import org.redisson.api.RTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.manager.helpers.RedisClient;

public class Manager {

  private static final Logger log = LoggerFactory.getLogger(Manager.class);
  
  public static void start() {
    log.info("Consumer manager is starting...");

    topic = RedisClient.createTopic(SysProps.REDIS_ACTIVE_LINKS_TOPIC());
    tPool = Executors.newFixedThreadPool(Props.ACTIVE_LINKS_CONSUMER_TPOOL_CAPACITY());

    topic.addListener(Link.class, new MessageListener<Link>(){
      public void onMessage(CharSequence channel, Link link) {
        tPool.submit(new ActiveLinkConsumer(link));
      };
    });

    log.info("Consumer manager is started.");
  }

  public static void shutdown() {
    try {
      topic.removeAllListeners();
      tPool.shutdown();
      tPool.awaitTermination(SysProps.WAITING_TIME_FOR_TERMINATION(), TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("Thread pool termination is interrupted.", e);
    }
  }


}
