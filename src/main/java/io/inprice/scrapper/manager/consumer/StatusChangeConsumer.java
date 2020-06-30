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
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.JsonConverter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.CompetitorRepository;

public class StatusChangeConsumer {

  private static final Logger log = LoggerFactory.getLogger(StatusChangeConsumer.class);
  private static final CompetitorRepository competitorRepository = Beans.getSingleton(CompetitorRepository.class);

  public void start() {
    log.info("Status change consumer is running.");

    final Channel channel = RabbitMQ.openChannel();

    final Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.STATUS_CHANGE_POOL.submit(() -> {
          try {
            StatusChange change = JsonConverter.fromJson(new String(body), StatusChange.class);
            if (change != null) {

              boolean isOK = competitorRepository.changeStatus(change);
              if (isOK) {
                //RedisClient.addPriceChanging(change.getCompetitor().getProductId());
              } else {
                log.error("DB problem while changing competitor status! " + change.getCompetitor().toString());
              }
            } else {
              log.error("Status change object is null!");
            }
          } catch (Exception e) {
            log.error("Failed to submit Tasks into ThreadPool", e);
          }
        });
      }
    };

    try {
      channel.basicConsume(SysProps.MQ_STATUS_CHANGE_QUEUE(), true, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue up for getting status changes.", e);
    }
  }

}
