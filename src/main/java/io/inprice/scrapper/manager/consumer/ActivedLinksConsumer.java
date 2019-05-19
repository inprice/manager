package io.inprice.scrapper.manager.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.Links;

import java.io.IOException;

public class ActivedLinksConsumer {

	private static final Logger log = new Logger(ActivedLinksConsumer.class);

	public static void start() {
		log.info("Activated links consumer is running.");

		final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, final byte[] body) {
				try {
					ThreadPools.ACTIVATED_LINKS_POOL.submit(() -> {
						Link link = Converter.toObject(body);
						if (link != null) {
							boolean isOK = Links.activate(link);
							if (! isOK) {
								log.error("DB problem while activating link!");
							} else {
								log.debug("A new link with id %d is successfully activated!", link.getId());
							}
						} else {
							log.error("Link is null!");
						}
					});
				} catch (Exception e) {
					log.error("Failed to submit a task into ThreadPool", e);
				}
			}
		};

		try {
			RabbitMQ.getChannel().basicConsume(Config.RABBITMQ_ACTIVATED_LINKS_QUEUE, true, consumer);
		} catch (IOException e) {
			log.error("Failed to set a queue up for getting activated links.", e);
		}
	}

}
