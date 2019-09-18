package io.inprice.scrapper.manager.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.info.PriceUpdateInfo;
import io.inprice.scrapper.manager.config.Properties;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.Links;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PriceChangeConsumer {

	private static final Logger log = LoggerFactory.getLogger(PriceChangeConsumer.class);
	private static final Properties props = Beans.getSingleton(Properties.class);

	public static void start() {
		log.info("Price change consumer is up and running.");

		final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
				try {
					ThreadPools.PRICE_CHANGE_POOL.submit(() -> {
						PriceUpdateInfo pui = Converter.toObject(body);
						if (pui != null) {
							boolean isOK = Links.changePrice(pui);
							if (isOK) {
								RedisClient.addPriceChanging(pui.getProductId());
							} else {
								log.error("DB problem while changing Price!");
							}
						} else {
							log.error("PriceUpdateInfo object is null!");
						}
					});
				} catch (Exception e) {
					log.error("Error in submitting Tasks into ThreadPool", e);
				}
			}
		};

		try {
			RabbitMQ.getChannel().basicConsume(props.getQueue_PriceChange(), true, consumer);
		} catch (IOException e) {
			log.error("Failed to set a queue up for getting price changes.", e);
		}
	}

}
