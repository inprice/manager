package io.inprice.scrapper.manager.consumer;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.MessageConverter;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;

public class DeletedLinksConsumer {

  private static final Logger log = LoggerFactory.getLogger(DeletedLinksConsumer.class);

  public static void start() {
    log.info("Deleted links consumer is up and running.");

    final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.DELETED_LINKS_POOL.submit(() -> {
          try {
            Long productId = MessageConverter.toObject(body);
            if (productId != null && productId > 0) {
              RedisClient.addPriceChanging(productId);
              //RabbitMQ.getChannel().basicAck(envelope.getDeliveryTag(), false);
            } else {
              log.error("Invalid product id value!");
            }
          } catch (Exception e) {
            log.error("Failed to submit Tasks into ThreadPool", e);
            /*
            try {
              RabbitMQ.getChannel().basicNack(envelope.getDeliveryTag(), false, false);
            } catch (IOException e1) {
              log.error("Failed to send a message to dlq", e1);
            }*/
          }
        });
      }
    };

    try {
      RabbitMQ.getChannel().basicConsume(SysProps.MQ_QUEUE_DELETED_LINKS(), true, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue up for deleted links.", e);
    }
  }

}
