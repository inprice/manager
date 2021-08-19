package io.inprice.manager.helpers;

public class RedisClient {
/*
  private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);

  private static BaseRedisClient baseClient;

  private static RTopic activeLinksTopic;
  private static RTopic statusChangeTopic;

  public static RQueue<AccessLog> accessLogQueue;
  
  static {
    baseClient = new BaseRedisClient();
    baseClient.open(() -> {
      activeLinksTopic = createTopic(SysProps.REDIS_ACTIVE_LINKS_TOPIC);
      statusChangeTopic = createTopic(SysProps.REDIS_STATUS_CHANGE_TOPIC);

      accessLogQueue = baseClient.getClient().getQueue(SysProps.REDIS_ACCESS_LOG_QUEUE);
    });
  }

  public static RTopic createTopic(String topic) {
    return baseClient.getClient().getTopic(topic);
  }

  TODO: platformun ait oldugu queue kullanilacak!!!
  public static void publishActiveLink(Link link) {
    if (baseClient.isHealthy()) {
      activeLinksTopic.publish(link);
    } else {
      logger.warn("Redis connection is not healthy. Publishing active link avoided! Status: {}, Url: {}", link.getStatus(), link.getUrl());
    }
  }

  public static void publishStatusChange(Link link, LinkStatus oldStatus) {
    if (baseClient.isHealthy()) {
      statusChangeTopic.publish(new LinkStatusChange(link, oldStatus, link.getPrice()));
    } else {
      logger.error("Redis seems not healty. Sending StatusChange message error! Status: {}, Url: {}", link.getStatus(), link.getUrl());
    }
  }

  public static void shutdown() {
    activeLinksTopic.removeAllListeners();
    statusChangeTopic.removeAllListeners();
    baseClient.shutdown();
  }
*/
}
