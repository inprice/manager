package io.inprice.scrapper.manager.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.info.ProductPriceInfo;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.common.info.PriceChange;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.Links;

import java.io.IOException;

public class LinkPriceChangeConsumer {

	private static final Logger log = new Logger(LinkPriceChangeConsumer.class);

	public static void start() {
		log.info("Link price change consumer is up and running.");

		final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
				try {
					ThreadPools.PRICE_CHANGE_POOL.submit(() -> {
						PriceChange change = Converter.toObject(body);
						if (change != null) {
							boolean isOK = Links.changePrice(change);
							if (isOK) {
								RedisClient.addPriceChanging(new ProductPriceInfo(change.getProductId(), change.getNewPrice()));
							} else {
								log.error("DB problem while changing Price!");
							}
						} else {
							log.error("PriceChange is null!");
						}
					});
				} catch (Exception e) {
					log.error("Error in submitting Tasks into ThreadPool", e);
				}
			}
		};

		try {
			RabbitMQ.getChannel().basicConsume(Config.RABBITMQ_PRICE_CHANGE_QUEUE, true, consumer);
		} catch (IOException e) {
			log.error("Failed to set a queue up for getting price changes.", e);
		}
	}

}
