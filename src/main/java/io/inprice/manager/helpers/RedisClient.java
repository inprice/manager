package io.inprice.manager.helpers;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.models.Link;

public class RedisClient {

  private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

  private static boolean isHealthy;
  private static RedissonClient client;
  private static RTopic linksTopic;

  static {
    final String redisPass = SysProps.REDIS_PASSWORD();
    Config config = new Config();
    config
      .useSingleServer()
      .setAddress(String.format("redis://%s:%d", SysProps.REDIS_HOST(), SysProps.REDIS_PORT()))
      .setPassword(!StringUtils.isBlank(redisPass) ? redisPass : null)
      .setConnectionPoolSize(10)
      .setConnectionMinimumIdleSize(1)
      .setIdleConnectionTimeout(5000)
      .setTimeout(5000);

    while (!isHealthy && Global.isApplicationRunning) {
      try {
        client = Redisson.create(config);
        linksTopic = client.getTopic(SysProps.REDIS_ACTIVE_LINKS_TOPIC());
        isHealthy = true;
      } catch (Exception e) {
        log.error("Failed to connect to Redis server, trying again in 3 seconds!", e.getMessage());
        try {
          Thread.sleep(3000);
        } catch (InterruptedException ignored) { }
      }
    }
        
  }

  public static RTopic createTopic(String topic) {
    return client.getTopic(topic);
  }

  public static void publish(Link link) {
    if (isHealthy) {
      linksTopic.publish(link);
    } else {
      log.warn("Redis connection is not healthy, so publishing messages avoided! Status: " + link.getStatus());
    }
  }

  public static void shutdown() {
    if (client != null) {
      client.shutdown();
    } else {
      log.warn("No redis client found!");
    }
  }

}
