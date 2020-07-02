package io.inprice.manager.consumer;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.PriceUpdateInfo;
import io.inprice.manager.helpers.RedisClient;
import io.inprice.manager.helpers.ThreadPools;
import io.inprice.manager.repository.CompetitorRepository;

public class PriceChangeConsumer {

  private static final Logger log = LoggerFactory.getLogger(PriceChangeConsumer.class);
  private static final CompetitorRepository competitorRepository = Beans.getSingleton(CompetitorRepository.class);

  public void start() {
    log.info("Price change consumer is up and running.");

    final Channel channel = RabbitMQ.openChannel();

    final Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.PRICE_CHANGE_POOL.submit(() -> {
          try {
            PriceUpdateInfo pui = JsonConverter.fromJson(new String(body), PriceUpdateInfo.class);
            if (pui != null) {
              boolean isOK = competitorRepository.changePrice(pui);
              if (isOK) {
                RedisClient.addPriceChanging(pui.getProductId());
              } else {
                log.error("DB problem while changing Price!");
              }
            } else {
              log.error("PriceUpdateInfo object is null!");
            }
          } catch (Exception e) {
            log.error("Failed to handle price changing", e);
          }
        });
      }
    };

    try {
      channel.basicConsume(SysProps.MQ_PRICE_CHANGE_QUEUE(), true, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue up for getting price changes.", e);
    }
  }

}
