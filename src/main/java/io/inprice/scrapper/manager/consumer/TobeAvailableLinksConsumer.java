package io.inprice.scrapper.manager.consumer;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.external.Props;
import io.inprice.scrapper.manager.helpers.MessageConverter;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.LinkRepository;

/**
 * Consumer for links to be available
 */
public class TobeAvailableLinksConsumer {

  private static final Logger log = LoggerFactory.getLogger(TobeAvailableLinksConsumer.class);
  private static final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);

  public static void start() {
    log.info("TO BE AVAILABLE links consumer is up and running.");

    final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.AVAILABLE_LINKS_POOL.submit(() -> {
          try {
            Link link = MessageConverter.toObject(body);
            if (link != null) {
              boolean isOK = linkRepository.makeAvailable(link);
              if (isOK) {
                RedisClient.addPriceChanging(link.getProductId());
                RabbitMQ.getChannel().basicAck(envelope.getDeliveryTag(), false);
              } else {
                log.error("DB problem while activating a link!");
              }
            } else {
              log.error("Link is null!");
            }
          } catch (Exception e) {
            log.error("Failed to submit Tasks into ThreadPool", e);
            try {
              RabbitMQ.getChannel().basicNack(envelope.getDeliveryTag(), false, false);
            } catch (IOException e1) {
              log.error("Failed to send a message to dlq", e1);
            }
          }
        });
      }
    };

    try {
      RabbitMQ.getChannel().basicConsume(Props.MQ_QUEUE_TOBE_AVAILABLE_LINKS(), true, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue for getting links to make available.", e);
    }
  }

}
