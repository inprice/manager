package io.inprice.scrapper.manager.helpers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.manager.config.Config;

import java.io.Serializable;

public class RabbitMQ {

	private static final Logger log = new Logger(RabbitMQ.class);

	private static Channel channel;

	public static Channel getChannel() {
		if (!isChannelActive()) {
			synchronized (log) {
				if (!isChannelActive()) {
					final ConnectionFactory connectionFactory = new ConnectionFactory();
					connectionFactory.setHost(Config.RABBITMQ_HOST);
					connectionFactory.setPort(Config.RABBITMQ_PORT);
					connectionFactory.setUsername(Config.RABBITMQ_USERNAME);
					connectionFactory.setPassword(Config.RABBITMQ_PASSWORD);

					try {
						Connection connection = connectionFactory.newConnection();
						channel = connection.createChannel();

						channel.exchangeDeclare(Config.RABBITMQ_LINK_EXCHANGE, "topic");
						channel.exchangeDeclare(Config.RABBITMQ_CHANGE_EXCHANGE, "topic");

						channel.queueDeclare(Config.RABBITMQ_NEW_LINKS_QUEUE, true, false, false, null);
						channel.queueDeclare(Config.RABBITMQ_AVAILABLE_LINKS_QUEUE, true, false, false, null);
						channel.queueDeclare(Config.RABBITMQ_FAILED_LINKS_QUEUE, true, false, false, null);
						channel.queueDeclare(Config.RABBITMQ_TOBE_AVAILABLE_LINKS_QUEUE, true, false, false, null);

						channel.queueDeclare(Config.RABBITMQ_STATUS_CHANGE_QUEUE, true, false, false, null);
						channel.queueDeclare(Config.RABBITMQ_PRICE_CHANGE_QUEUE, true, false, false, null);
						channel.queueDeclare(Config.RABBITMQ_DELETED_LINKS_QUEUE, true, false, false, null);

						channel.queueBind(Config.RABBITMQ_NEW_LINKS_QUEUE, Config.RABBITMQ_LINK_EXCHANGE, Config.RABBITMQ_NEW_LINKS_QUEUE + ".#");
						channel.queueBind(Config.RABBITMQ_FAILED_LINKS_QUEUE, Config.RABBITMQ_LINK_EXCHANGE, Config.RABBITMQ_FAILED_LINKS_QUEUE + ".#");
						channel.queueBind(Config.RABBITMQ_AVAILABLE_LINKS_QUEUE, Config.RABBITMQ_LINK_EXCHANGE, Config.RABBITMQ_AVAILABLE_LINKS_QUEUE + ".#");
						channel.queueBind(Config.RABBITMQ_TOBE_AVAILABLE_LINKS_QUEUE, Config.RABBITMQ_LINK_EXCHANGE, Config.RABBITMQ_TOBE_AVAILABLE_LINKS_QUEUE + ".#");

						channel.queueBind(Config.RABBITMQ_STATUS_CHANGE_QUEUE, Config.RABBITMQ_CHANGE_EXCHANGE, Config.RABBITMQ_STATUS_CHANGE_QUEUE + ".#");
						channel.queueBind(Config.RABBITMQ_PRICE_CHANGE_QUEUE, Config.RABBITMQ_CHANGE_EXCHANGE, Config.RABBITMQ_PRICE_CHANGE_QUEUE + ".#");
						channel.queueBind(Config.RABBITMQ_DELETED_LINKS_QUEUE, Config.RABBITMQ_CHANGE_EXCHANGE, Config.RABBITMQ_DELETED_LINKS_QUEUE + ".#");
					} catch (Exception e) {
						log.error("Error in opening RabbitMQ channel", e);
					}
				}
			}
		}

		return channel;
	}

	public static boolean publish(String queue, Serializable message) {
		return publish(Config.RABBITMQ_LINK_EXCHANGE, queue, message);
	}

	public static boolean publish(String exchange, String queue, Serializable message) {
		try {
			channel.basicPublish(exchange, queue, null, Converter.fromObject(message));
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
