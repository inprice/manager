package io.inprice.scrapper.manager.consumer;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.manager.external.Props;
import io.inprice.scrapper.manager.helpers.MessageConverter;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.repository.LinkRepository;

public class StatusChangeConsumer {

  private static final Logger log = LoggerFactory.getLogger(StatusChangeConsumer.class);
  private static final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);

  public static void start() {
    log.info("Status change consumer is running.");

    final Consumer consumer = new DefaultConsumer(RabbitMQ.getChannel()) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.STATUS_CHANGE_POOL.submit(() -> {
          try {
            StatusChange change = MessageConverter.toObject(body);
            if (change != null) {
              boolean isOK = linkRepository.changeStatus(change);
              if (isOK) {
                RedisClient.addPriceChanging(change.getLink().getProductId());
                RabbitMQ.getChannel().basicAck(envelope.getDeliveryTag(), false);
              } else {
                log.error("DB problem while changing Link status!");
              }
            } else {
              log.error("Status change object is null!");
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
      RabbitMQ.getChannel().basicConsume(Props.MQ_QUEUE_STATUS_CHANGE(), false, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue up for getting status changes.", e);
    }
  }

}
