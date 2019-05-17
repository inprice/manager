package io.inprice.scrapper.manager.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.info.LinkStatusChange;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.Links;

import java.io.IOException;

public class NewToActiveLinkConsumer {

	private static final Logger log = new Logger(NewToActiveLinkConsumer.class);

	public static void start() {
		log.info("NewToActiveLinkConsumer is running.");

		final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, final byte[] body) {
				try {
					ThreadPools.STATUS_CHANGE_POOL.submit(() -> {
						Link link = Converter.toObject(body);
						if (link != null) {
							//TODO: bu satirdan sonrasi tamamen duzenlenmeli
							//TODO: worker tarafina gonderilmis NEW statusundeki linkler, basarili bir sekilde parse edilip ACTIVE statusune gecirilmek uzere buraya gelecekler
							boolean isOK = Links.changeStatus(change);
							if (! isOK) {
								log.error("DB problem while changing LinkStatus!");
							}
						} else {
							log.error("LinkStatusChange is null!");
						}
					});
				} catch (Exception e) {
					log.error("Error in submitting Tasks into ThreadPool", e);
				}
			}
		};

		try {
			RabbitMQ.getChannel().basicConsume("new-to-active-queue", true, consumer);
		} catch (IOException e) {
			log.error("Failed to set a queue up for getting status changes.", e);
		}
	}

}
