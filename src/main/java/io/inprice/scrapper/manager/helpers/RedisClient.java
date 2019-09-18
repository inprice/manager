package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.manager.config.Properties;
import org.redisson.Redisson;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisClient {

	private static final Properties props = Beans.getSingleton(Properties.class);

	private static final RedissonClient client;
	private static final RSet<Long> priceChangingProductsIdSet;

	static {
		Config config = new Config();
		config
			.useSingleServer()
			.setAddress(String.format("redis://%s:%d", props.getRedis_Host(), props.getRedis_Port()))
			.setPassword((props.getRedis_Password().trim().isEmpty() ? null : props.getRedis_Password()));

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
