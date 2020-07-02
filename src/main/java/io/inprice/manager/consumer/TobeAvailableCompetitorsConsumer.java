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
import io.inprice.common.models.Competitor;
import io.inprice.manager.helpers.ThreadPools;
import io.inprice.manager.repository.CompetitorRepository;

/**
 * Consumer for competitors to be available
 */
public class TobeAvailableCompetitorsConsumer {

  private static final Logger log = LoggerFactory.getLogger(TobeAvailableCompetitorsConsumer.class);
  private static final CompetitorRepository competitorRepository = Beans.getSingleton(CompetitorRepository.class);

  public void start() {
    log.info("TO BE AVAILABLE competitors consumer is up and running.");

    final Channel channel = RabbitMQ.openChannel();

    final Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.AVAILABLE_COMPETITORS_POOL.submit(() -> {
          try {
            Competitor competitor = JsonConverter.fromJson(new String(body), Competitor.class);
            if (competitor != null) {
              boolean isOK = competitorRepository.makeAvailable(competitor);
              if (isOK) {
                RabbitMQ.publish(
                  channel, SysProps.MQ_CHANGES_EXCHANGE(), SysProps.MQ_PRICE_REFRESH_ROUTING(), competitor.getProductId().toString()
                );
              } else {
                log.error("DB problem while activating a competitor!");
              }
            } else {
              log.error("competitor is null!");
            }
          } catch (Exception e) {
            log.error("Failed to submit Tasks into ThreadPool", e);
          }
        });
      }
    };

    try {
      channel.basicConsume(SysProps.MQ_TOBE_AVAILABLE_COMPETITORS_QUEUE(), true, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue for getting competitors to make available.", e);
    }
  }

}
