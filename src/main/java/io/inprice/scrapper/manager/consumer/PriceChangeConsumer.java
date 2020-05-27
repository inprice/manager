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
import io.inprice.scrapper.common.info.PriceUpdateInfo;
import io.inprice.scrapper.manager.helpers.Beans;
import io.inprice.scrapper.manager.helpers.MessageConverter;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.LinkRepository;

public class PriceChangeConsumer {

  private static final Logger log = LoggerFactory.getLogger(PriceChangeConsumer.class);
  private static final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);

  public static void start() {
    log.info("Price change consumer is up and running.");

    final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.PRICE_CHANGE_POOL.submit(() -> {
          try {
            PriceUpdateInfo pui = MessageConverter.toObject(body);
            if (pui != null) {
              boolean isOK = linkRepository.changePrice(pui);
              if (isOK) {
                RedisClient.addPriceChanging(pui.getProductId());
                //RabbitMQ.getChannel().basicAck(envelope.getDeliveryTag(), false);
              } else {
                log.error("DB problem while changing Price!");
              }
            } else {
              log.error("PriceUpdateInfo object is null!");
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
      RabbitMQ.getChannel().basicConsume(SysProps.MQ_PRICE_CHANGE_QUEUE(), false, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue up for getting price changes.", e);
    }
  }

}
