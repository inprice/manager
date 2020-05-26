package io.inprice.scrapper.manager.helpers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.manager.external.Props;

public class RabbitMQ {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQ.class);

  private static Channel channel;

  public static Channel getChannel() {
    if (!isChannelActive()) {
      synchronized (log) {
        if (!isChannelActive()) {
          final ConnectionFactory connectionFactory = new ConnectionFactory();
          connectionFactory.setHost(Props.MQ_HOST());
          connectionFactory.setPort(Props.MQ_PORT());
          connectionFactory.setUsername(Props.MQ_USERNAME());
          connectionFactory.setPassword(Props.MQ_PASSWORD());

          try {
            final String newLinksQueue = Props.MQ_ROUTING_NEW_LINKS();
            final String availableLinksQueue = Props.MQ_ROUTING_AVAILABLE_LINKS();
            final String failedLinksQueue = Props.MQ_ROUTING_FAILED_LINKS();

            final String tobeAvailableLinksQueue = Props.MQ_QUEUE_TOBE_AVAILABLE_LINKS();

            final String statusChangeQueue = Props.MQ_QUEUE_STATUS_CHANGE();
            final String priceChangeQueue = Props.MQ_QUEUE_PRICE_CHANGE();
            final String deletedLinksQueue = Props.MQ_QUEUE_DELETED_LINKS();

            Connection connection = connectionFactory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(Props.MQ_EXCHANGE_LINKS(), "topic");
            channel.exchangeDeclare(Props.MQ_EXCHANGE_CHANGES(), "topic");
						channel.exchangeDeclare(Props.MQ_EXCHANGE_DEAD_LETTER(), "direct");

            Map<String, Object> args = new HashMap<String, Object>();
            args.put("x-dead-letter-exchange", Props.MQ_EXCHANGE_DEAD_LETTER());

            channel.queueDeclare(newLinksQueue, true, false, false, args);
            channel.queueDeclare(availableLinksQueue, true, false, false, args);
            channel.queueDeclare(failedLinksQueue, true, false, false, args);
            channel.queueDeclare(tobeAvailableLinksQueue, true, false, false, args);
            channel.queueDeclare(statusChangeQueue, true, false, false, args);
            channel.queueDeclare(priceChangeQueue, true, false, false, args);
            channel.queueDeclare(deletedLinksQueue, true, false, false, args);

            channel.queueBind(newLinksQueue, Props.MQ_EXCHANGE_LINKS(), newLinksQueue + ".#");
            channel.queueBind(availableLinksQueue, Props.MQ_EXCHANGE_LINKS(), availableLinksQueue + ".#");
            channel.queueBind(failedLinksQueue, Props.MQ_EXCHANGE_LINKS(), failedLinksQueue + ".#");
            channel.queueBind(tobeAvailableLinksQueue, Props.MQ_EXCHANGE_LINKS(), tobeAvailableLinksQueue + ".#");
            channel.queueBind(statusChangeQueue, Props.MQ_EXCHANGE_CHANGES(), statusChangeQueue + ".#");
            channel.queueBind(priceChangeQueue, Props.MQ_EXCHANGE_CHANGES(), priceChangeQueue + ".#");
            channel.queueBind(deletedLinksQueue, Props.MQ_EXCHANGE_CHANGES(), deletedLinksQueue + ".#");
          } catch (Exception e) {
            log.error("Failed to establishing RabbitMQ channel", e);
          }
        }
      }
    }

    return channel;
  }

  public static boolean publish(String routingKey, Serializable message) {
    return publish(Props.MQ_EXCHANGE_LINKS(), routingKey, message);
  }

  public static boolean publish(String exchange, String routingKey, Serializable message) {
    try {
      channel.basicPublish(exchange, routingKey, null, MessageConverter.fromObject(message));
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
