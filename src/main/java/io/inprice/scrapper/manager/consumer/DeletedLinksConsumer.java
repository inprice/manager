package io.inprice.scrapper.manager.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.info.PriceUpdateInfo;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DeletedLinksConsumer {

	private static final Logger log = LoggerFactory.getLogger(DeletedLinksConsumer.class);

	public static void start() {
		log.info("Deleted links consumer is up and running.");

		final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
				try {
					ThreadPools.DELETED_LINKS_POOL.submit(() -> {
						PriceUpdateInfo pui = Converter.toObject(body);
						if (pui != null) {
							RedisClient.addPriceChanging(pui.getProductId());
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
			RabbitMQ.getChannel().basicConsume(Config.MQ_DELETED_LINKS_QUEUE, true, consumer);
		} catch (IOException e) {
			log.error("Failed to set a queue up for deleted links.", e);
		}
	}

}
