package io.inprice.scrapper.manager.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.manager.config.Properties;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.LinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StatusChangeConsumer {

	private static final Logger log = LoggerFactory.getLogger(StatusChangeConsumer.class);
	private static final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);
	private static final Properties props = Beans.getSingleton(Properties.class);

	public static void start() {
		log.info("Status change consumer is running.");

		final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
				try {
					ThreadPools.STATUS_CHANGE_POOL.submit(() -> {
						StatusChange change = Converter.toObject(body);
						if (change != null) {
							boolean isOK = linkRepository.changeStatus(change);
							if (isOK) {
								RedisClient.addPriceChanging(change.getLink().getProductId());
							} else {
								log.error("DB problem while changing Link status!");
							}
						} else {
							log.error("Status change object is null!");
						}
					});
				} catch (Exception e) {
					log.error("Failed to submit a tasks into ThreadPool", e);
				}
			}
		};

		try {
			RabbitMQ.getChannel().basicConsume(props.getQueue_StatusChange(), true, consumer);
		} catch (IOException e) {
			log.error("Failed to set a queue up for getting status changes.", e);
		}
	}

}
