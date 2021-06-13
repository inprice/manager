package io.inprice.manager.helpers;

import org.redisson.api.RTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.BaseRedisClient;
import io.inprice.common.info.LinkStatusChange;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;

public class RedisClient {

  private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

  private static BaseRedisClient baseClient;
  private static RTopic activeLinksTopic;
  private static RTopic statusChangeTopic;

  static {
    baseClient = new BaseRedisClient();
    baseClient.open(() -> {
      activeLinksTopic = createTopic(SysProps.REDIS_ACTIVE_LINKS_TOPIC);
      statusChangeTopic = createTopic(SysProps.REDIS_STATUS_CHANGE_TOPIC);
    });
  }

  public static RTopic createTopic(String topic) {
    return baseClient.getClient().getTopic(topic);
  }

  public static void publishActiveLink(Link link) {
    if (baseClient.isHealthy()) {
      activeLinksTopic.publish(link);
    } else {
      log.warn("Redis connection is not healthy. Publishing active link avoided! Status: {}, Url: {}", link.getStatus(), link.getUrl());
    }
  }

  public static void publishStatusChange(Link link, LinkStatus oldStatus) {
    if (baseClient.isHealthy()) {
      statusChangeTopic.publish(new LinkStatusChange(link, oldStatus, link.getPrice()));
    } else {
      log.error("Redis seems not healty. Sending StatusChange message error! Status: {}, Url: {}", link.getStatus(), link.getUrl());
    }
  }

  public static void shutdown() {
    activeLinksTopic.removeAllListeners();
    statusChangeTopic.removeAllListeners();
    baseClient.shutdown();
  }

}
