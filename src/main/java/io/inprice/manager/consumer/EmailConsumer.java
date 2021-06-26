package io.inprice.manager.consumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RTopic;

import io.inprice.common.config.SysProps;
import io.inprice.common.info.EmailData;
import io.inprice.manager.email.EmailSender;
import io.inprice.manager.helpers.RedisClient;

/**
 * Designed to manage all the sending emails around the platform
 * 
 * @author mdpinar
 * @since 2020-06-20
 */
public class EmailConsumer {

  private static RTopic topic;
  private static ExecutorService tPool;

  public static void start() {
  	tPool = Executors.newFixedThreadPool(SysProps.TPOOL_EMAIL_CONSUMER_CAPACITY);

  	topic = RedisClient.createTopic(SysProps.REDIS_SENDING_EMAILS_TOPIC);
  	topic.addListener(EmailData.class, (channel, emailData) -> tPool.submit(() -> EmailSender.send(emailData)));
  }

  public static void stop() {
    try {
      topic.removeAllListeners();
      tPool.shutdown();
      tPool.awaitTermination(SysProps.WAITING_TIME_FOR_TERMINATION, TimeUnit.SECONDS);
    } catch (InterruptedException e) { }
  }
  
}
