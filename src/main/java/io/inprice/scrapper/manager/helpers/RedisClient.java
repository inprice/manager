package io.inprice.scrapper.manager.helpers;

import org.redisson.Redisson;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.manager.external.Props;

public class RedisClient {

  private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

  private static boolean isHealthy;
  private static RedissonClient client;

  private static RSet<Long> priceChangingProductsIdSet;

  static {
    Config config = new Config();
    config.useSingleServer().setAddress(String.format("redis://%s:%d", Props.REDIS_HOST(), Props.REDIS_PORT()))
        .setPassword((Props.REDIS_PASSWORD().trim().isEmpty() ? null : Props.REDIS_PASSWORD()));

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
    client.shutdown();
  }

}
