package io.inprice.scrapper.manager.consumer;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.utils.NumberUtils;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;

public class DeletedLinksConsumer {

  private static final Logger log = LoggerFactory.getLogger(DeletedLinksConsumer.class);

  public static void start() {
    log.info("Deleted links consumer is up and running.");

    final Channel channel = RabbitMQ.openChannel();

    final Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.DELETED_LINKS_POOL.submit(() -> {
          try {
            Long productId = NumberUtils.toLong(new String(body));
            if (productId != null && productId > 0) {
              RedisClient.addPriceChanging(productId);
              channel.basicAck(envelope.getDeliveryTag(), false);
            } else {
              log.error("Invalid product id value!");
            }
          } catch (Exception e) {
            log.error("Failed to submit Tasks into ThreadPool", e);
            try {
              channel.basicNack(envelope.getDeliveryTag(), false, false);
            } catch (IOException e1) {
              log.error("Failed to send a message to dlq", e1);
            }
          }
        });
      }
    };

    try {
      channel.basicConsume(SysProps.MQ_DELETED_LINKS_QUEUE(), false, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue up for deleted links.", e);
    }
  }

}
