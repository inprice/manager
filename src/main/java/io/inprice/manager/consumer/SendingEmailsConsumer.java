package io.inprice.manager.consumer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.inprice.common.config.QueueDef;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.EmailData;
import io.inprice.manager.helpers.EmailSender;

/**
 * Designed to manage all the sending emails around the platform
 * 
 * @author mdpinar
 * @since 2020-06-20
 */
class SendingEmailsConsumer {

  private static final Logger logger = LoggerFactory.getLogger(SendingEmailsConsumer.class);
  
  SendingEmailsConsumer(QueueDef queueDef) throws IOException {
  	String forWhichConsumer = "MAN-CON: " + queueDef.NAME;

  	Connection conn = RabbitMQ.createConnection(forWhichConsumer, queueDef.CAPACITY);
		Channel channel = conn.createChannel();

		Consumer consumer = new DefaultConsumer(channel) {
  		@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
	      try {
					EmailSender.send(
						JsonConverter.fromJsonWithoutJsonIgnore(new String(body), EmailData.class)
					);				
	      
	      } catch (Exception e) {
    			channel.basicAck(envelope.getDeliveryTag(), false);
		      logger.error("Failed to send email. " + body, e);
		    }
			}
		};

		logger.info(forWhichConsumer + " is up and running.");
		channel.basicConsume(queueDef.NAME, true, consumer);
  }

}
