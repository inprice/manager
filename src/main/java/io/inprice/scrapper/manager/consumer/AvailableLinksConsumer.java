package io.inprice.scrapper.manager.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.Links;

import java.io.IOException;

/**
 * Consumer for links to be available
 */
public class AvailableLinksConsumer {

	private static final Logger log = new Logger(AvailableLinksConsumer.class);

	public static void start() {
		log.info("From [NEW, RENEWED] to AVAILABLE links consumer is up and running.");

		final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, final byte[] body) {
				try {
					ThreadPools.AVAILABLE_LINKS_POOL.submit(() -> {
						Link link = Converter.toObject(body);
						if (link != null) {
							boolean isOK = Links.makeAvailable(link);
							if (! isOK) {
								log.error("DB problem while activating a link!");
							} else {
								log.debug("Link with id %d is successfully made available!", link.getId());
							}
						} else {
							log.error("Link is null!");
						}
					});
				} catch (Exception e) {
					log.error("Failed to submit a tasks into ThreadPool", e);
				}
			}
		};

		try {
			RabbitMQ.getChannel().basicConsume(Config.RABBITMQ_AVAILABLE_LINKS_QUEUE, true, consumer);
		} catch (IOException e) {
			log.error("Failed to set a queue for getting links to make available.", e);
		}
	}

}
