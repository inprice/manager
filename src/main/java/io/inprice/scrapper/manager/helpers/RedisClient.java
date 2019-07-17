package io.inprice.scrapper.manager.helpers;

import org.redisson.Redisson;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisClient {

	private static final RedissonClient client;
	private static final RSet<Long> priceChangingProductsIdSet;

	static {
		Config config = new Config();
		config
			.useSingleServer()
			.setAddress(String.format("redis://%s:%d", io.inprice.scrapper.manager.config.Config.REDIS_HOST, io.inprice.scrapper.manager.config.Config.REDIS_PORT))
			.setPassword(io.inprice.scrapper.manager.config.Config.REDIS_PASSWORD);

		client = Redisson.create(config);

		priceChangingProductsIdSet = client.getSet("PRICE-CHANGING_PRODUCTS-ID");
	}

	public static void addPriceChanging(Long id) {
		priceChangingProductsIdSet.add(id);
	}

	public static Long pollPriceChanging() {
		if (! priceChangingProductsIdSet.isEmpty())
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
