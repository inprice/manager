package io.inprice.scrapper.manager.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Properties;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.LinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Consumer for links to be available
 */
public class TobeAvailableLinksConsumer {

	private static final Logger log = LoggerFactory.getLogger(TobeAvailableLinksConsumer.class);
	private static final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);
	private static final Properties props = Beans.getSingleton(Properties.class);

	public static void start() {
		log.info("TO BE AVAILABLE links consumer is up and running.");

		final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
				try {
					ThreadPools.AVAILABLE_LINKS_POOL.submit(() -> {
						Link link = Converter.toObject(body);
						if (link != null) {
							boolean isOK = linkRepository.makeAvailable(link);
							if (isOK) {
								RedisClient.addPriceChanging(link.getProductId());
							} else {
								log.error("DB problem while activating a link!");
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
			RabbitMQ.getChannel().basicConsume(props.getQueue_TobeAvailableLinks(), true, consumer);
		} catch (IOException e) {
			log.error("Failed to set a queue for getting links to make available.", e);
		}
	}

}
