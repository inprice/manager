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
import io.inprice.scrapper.common.models.Competitor;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.CompetitorRepository;

/**
 * Consumer for competitors to be available
 */
public class TobeAvailableCompetitorsConsumer {

  private static final Logger log = LoggerFactory.getLogger(TobeAvailableCompetitorsConsumer.class);
  private static final CompetitorRepository competitorRepository = Beans.getSingleton(CompetitorRepository.class);

  public static void start() {
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

                // first method is much efficient in terms of db
                // RedisClient.addPriceChanging(competitor.getProductId());
                RabbitMQ.publish(channel, SysProps.MQ_CHANGES_EXCHANGE(), SysProps.MQ_PRICE_REFRESH_ROUTING(), competitor.getProductId().toString());

                channel.basicAck(envelope.getDeliveryTag(), false);
              } else {
                log.error("DB problem while activating a competitor!");
              }
            } else {
              log.error("competitor is null!");
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
      channel.basicConsume(SysProps.MQ_TOBE_AVAILABLE_COMPETITORS_QUEUE(), false, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue for getting competitors to make available.", e);
    }
  }

}
