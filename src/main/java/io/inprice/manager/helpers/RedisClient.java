package io.inprice.manager.helpers;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;

public class RedisClient {

  private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

  private static boolean isHealthy;
  private static RedissonClient client;

  private static RSet<Long> priceChangingProductsIdSet;

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

        priceChangingProductsIdSet = client.getSet("manager:price-changing:product-ips");
        isHealthy = true;
      } catch (Exception e) {
        log.error("Failed to connect to Redis server, trying again in 3 seconds!", e.getMessage());
        try {
          Thread.sleep(3000);
        } catch (InterruptedException ignored) { }
      }
    }
        
  }

  public static void addPriceChanging(Long id) {
    priceChangingProductsIdSet.add(id);
  }

  public static Long pollPriceChanging() {
    if (!priceChangingProductsIdSet.isEmpty())
      return priceChangingProductsIdSet.removeRandom();
    else
      return null;
  }

  public static boolean isPriceChangingSetEmpty() {
    return priceChangingProductsIdSet.isEmpty();
  }

  public static void shutdown() {
    if (client != null) {
      client.shutdown();
    } else {
      log.warn("No redis client found!");
    }
  }

}
