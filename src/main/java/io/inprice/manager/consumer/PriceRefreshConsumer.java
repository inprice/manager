package io.inprice.manager.consumer;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.models.ProductPrice;
import io.inprice.common.utils.NumberUtils;
import io.inprice.manager.helpers.ThreadPools;
import io.inprice.manager.repository.ProductRepository;

public class PriceRefreshConsumer {

  private static final Logger log = LoggerFactory.getLogger(PriceRefreshConsumer.class);
  private static final Database db = Beans.getSingleton(Database.class);
  private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

  public void start() {
    log.info("Price refresh consumer is up and running.");

    final Channel channel = RabbitMQ.openChannel();

    final Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.PRICE_REFRESH_POOL.submit(() -> {
          try {
            Long productId = NumberUtils.toLong(new String(body));
            if (productId != null && productId > 0) {
              refreshPrice(productId);
            } else {
              log.error("Invalid product id value!");
            }
          } catch (Exception e) {
            log.error("Failed to handle price refreshing", e);
          }
        });
      }
    };

    try {
      channel.basicConsume(SysProps.MQ_PRICE_REFRESH_QUEUE(), true, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue up for deleted competitors.", e);
    }
  }

  private static boolean refreshPrice(Long productId) {
    boolean result = false;

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      String zeroize = "";
      List<ProductPrice> ppList = new ArrayList<>();

      log.info("Price refresher is started for Id: {}", productId);
      ProductPrice pi = productRepository.getProductPrice(con, productId);
      if (pi != null) {
        ppList.add(pi);
      } else { // product and product_price relation must be removed since there is no price info
        zeroize = ""+productId;
      }
      log.info("Price refresher is completed for Id: {}", productId);

      if (ppList.size() > 0) {
        boolean res = productRepository.updatePrice(con, ppList, zeroize);
        if (res) {
          log.info("Prices of {} products have been updated!", ppList.size());
        } else {
          log.warn("An error occurred during updating products' prices!");
        }
      } else if (StringUtils.isNotBlank(zeroize)) {
        boolean res = productRepository.updatePrice(con, null, zeroize);
        if (res) {
          log.info("{} products' price info is zeroized. For Id: {}", zeroize);
        } else {
          log.warn("An error occurred during zeroizing products' price info!");
        }
      } else {
        log.info("No product price is updated!");
      }

      db.commit(con);
      result = true;

    } catch (Exception e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to update product price!", e);
    } finally {
      if (con != null)
        db.close(con);
    }

    return result;
  }

}
