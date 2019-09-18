package io.inprice.scrapper.manager.helpers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.manager.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class RabbitMQ {

	private static final Logger log = LoggerFactory.getLogger(RabbitMQ.class);
	private static final Properties props = Beans.getSingleton(Properties.class);

	private static Channel channel;

	public static Channel getChannel() {
		if (!isChannelActive()) {
			synchronized (log) {
				if (!isChannelActive()) {
					final ConnectionFactory connectionFactory = new ConnectionFactory();
					connectionFactory.setHost(props.getMQ_Host());
					connectionFactory.setPort(props.getMQ_Port());
					connectionFactory.setUsername(props.getMQ_Username());
					connectionFactory.setPassword(props.getMQ_Password());

					try {
						final String newLinksQueue = props.getRoutingKey_NewLinks();
						final String availableLinksQueue = props.getRoutingKey_AvailableLinks();
						final String failedLinksQueue = props.getRoutingKey_FailedLinks();

						final String tobeAvailableLinksQueue = props.getQueue_TobeAvailableLinks();

						final String statusChangeQueue = props.getQueue_StatusChange();
						final String priceChangeQueue = props.getQueue_PriceChange();
						final String deletedLinksQueue = props.getQueue_DeletedLinks();

						Connection connection = connectionFactory.newConnection();
						channel = connection.createChannel();

						channel.exchangeDeclare(props.getMQ_LinkExchange(), "topic");
						channel.exchangeDeclare(props.getMQ_ChangeExchange(), "topic");

						channel.queueDeclare(newLinksQueue, true, false, false, null);
						channel.queueDeclare(availableLinksQueue, true, false, false, null);
						channel.queueDeclare(failedLinksQueue, true, false, false, null);
						channel.queueDeclare(tobeAvailableLinksQueue, true, false, false, null);
						channel.queueDeclare(statusChangeQueue, true, false, false, null);
						channel.queueDeclare(priceChangeQueue, true, false, false, null);
						channel.queueDeclare(deletedLinksQueue, true, false, false, null);

						channel.queueBind(newLinksQueue, props.getMQ_LinkExchange(), newLinksQueue + ".#");
						channel.queueBind(availableLinksQueue, props.getMQ_LinkExchange(), availableLinksQueue + ".#");
						channel.queueBind(failedLinksQueue, props.getMQ_LinkExchange(), failedLinksQueue + ".#");
						channel.queueBind(tobeAvailableLinksQueue, props.getMQ_LinkExchange(), tobeAvailableLinksQueue + ".#");
						channel.queueBind(statusChangeQueue, props.getMQ_ChangeExchange(), statusChangeQueue + ".#");
						channel.queueBind(priceChangeQueue, props.getMQ_ChangeExchange(), priceChangeQueue + ".#");
						channel.queueBind(deletedLinksQueue, props.getMQ_ChangeExchange(), deletedLinksQueue + ".#");
					} catch (Exception e) {
						log.error("Error in opening RabbitMQ channel", e);
					}
				}
			}
		}

		return channel;
	}

	public static boolean publish(String routingKey, Serializable message) {
		return publish(props.getMQ_LinkExchange(), routingKey, message);
	}

	public static boolean publish(String exchange, String routingKey, Serializable message) {
		try {
			channel.basicPublish(exchange, routingKey, null, Converter.fromObject(message));
			return true;
		} catch (Exception e) {
			log.error("Failed to send a message to queue", e);
			return false;
		}
	}

	public static void closeChannel() {
		try {
			if (isChannelActive()) {
				channel.close();
			}
		} catch (Exception e) {
			log.error("Error while RabbitMQ.channel is closed.", e);
		}
	}

	public static boolean isChannelActive() {
		return (channel != null && channel.isOpen());
	}

}
