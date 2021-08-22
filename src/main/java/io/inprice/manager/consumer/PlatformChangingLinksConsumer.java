package io.inprice.manager.consumer;

import java.io.IOException;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.inprice.common.config.QueueDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.LinkPlatformChange;
import io.inprice.manager.dao.LinkDao;

/**
 * Publishes platform changes
 * 
 * @author mdpinar
 * @since 2021-08-22
 */
class PlatformChangingLinksConsumer {

  private static final Logger logger = LoggerFactory.getLogger(PlatformChangingLinksConsumer.class);
  
  PlatformChangingLinksConsumer(QueueDef queueDef) throws IOException {
  	String forWhichConsumer = "MAN-CON: " + queueDef.NAME;

  	Connection conn = RabbitMQ.createConnection(forWhichConsumer, queueDef.CAPACITY);
		Channel channel = conn.createChannel();

		Consumer consumer = new DefaultConsumer(channel) {
  		@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        try (Handle handle = Database.getHandle()) {
        	LinkPlatformChange change = JsonConverter.fromJson(new String(body, "UTF-8"), LinkPlatformChange.class);
        	LinkDao dao = handle.attach(LinkDao.class);
        	dao.setPlatform(change.getLinkId(), change.getNewPlatformId(), change.getStatus());
	      } catch (Exception e) {
		      logger.error("Failed to set platform id", e);
		    }
			}
		};

		logger.info(forWhichConsumer + " is up and running.");
		channel.basicConsume(queueDef.NAME, true, consumer);
  }

}
