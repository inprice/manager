package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.info.ProductPriceInfo;
import org.redisson.Redisson;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisClient {

	private static final RedissonClient client;
	private static final RSet<ProductPriceInfo> productPriceInfoSet;

	static {
		Config config = new Config();
		config
			.useSingleServer()
			.setAddress(String.format("redis://%s:%d", io.inprice.scrapper.manager.config.Config.REDIS_HOST, io.inprice.scrapper.manager.config.Config.REDIS_PORT))
			.setPassword(io.inprice.scrapper.manager.config.Config.REDIS_PASSWORD);

		client = Redisson.create(config);

		productPriceInfoSet = client.getSet("PRODUCT-PRICE-INFO");
	}

	public static void addProductPriceInfo(ProductPriceInfo ppi) {
		productPriceInfoSet.add(ppi);
	}

	public static ProductPriceInfo pollProductPriceInfo() {
		if (! productPriceInfoSet.isEmpty())
			return productPriceInfoSet.removeRandom();
		else
			return null;
	}

	public static boolean isProductPriceInfoSetEmpty() {
		return productPriceInfoSet.isEmpty();
	}

	public static void shutdown() {
		client.shutdown();
	}

}
