package io.inprice.manager.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.redisson.api.RTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.StatusChange;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.repository.CommonRepository;
import io.inprice.manager.config.Props;
import io.inprice.manager.helpers.RedisClient;

/**
 * Consumes all the parsed links by the parser
 * Mainly handles two operations; status and price change.
 * 
 * @author mdpinar
 * @since 2020-10-18
 */
public class ParsedLinksConsumer {

  private static final Logger logger = LoggerFactory.getLogger(ParsedLinksConsumer.class);

  private static final RTopic topic = RedisClient.createTopic(SysProps.REDIS_STATUS_CHANGE_TOPIC());
  //since parallel status changes for the links of one product can cause wrongly calculated fields such as avg and mac prices
  //this thread pool must be the capacity of 1
  private static final ExecutorService tPool = Executors.newFixedThreadPool(1);
  
  public static void start() {
    topic.addListener(StatusChange.class, (channel, change) -> {

      tPool.submit(new Runnable() {
        @Override
        public void run() {

          Link link = change.getLink();
          List<String> queries = new ArrayList<>();

          boolean isStatusChanged = (! change.getOldStatus().equals(link.getStatus()));
          boolean isNowAvailable = (LinkStatus.AVAILABLE.equals(link.getStatus()));
          boolean wasPreviouslyAvailable = (LinkStatus.AVAILABLE.equals(link.getPreStatus()));
          boolean isFailing = LinkStatus.FAILED_GROUP.equals(link.getStatus().getGroup());
          
          final boolean[] willPriceBeRefreshed = { (! change.getOldPrice().equals(link.getPrice())) };

          if (isStatusChanged) {
            try {
              if (isNowAvailable) {
                queries.add(generateMakeAvailableQuery(link));
                queries.addAll(generateRefreshSpecListQueries(link));
              } else {
                if (isFailing) {
                  queries.add(generateFailingQuery(link));
                } else { // must be passive
                  queries.add(generateJustStatusUpdateQuery(link));
                }
                willPriceBeRefreshed[0] = wasPreviouslyAvailable;
              }
              queries.add(generateLinkHistoryQuery(link));
            } catch (Exception e) {
              logger.error("Failed to generate queries for status change", e);
            }
          } 
          
          try (Handle handle = Database.getHandle()) {
            handle.inTransaction(transactional -> {
              if (queries.size() > 0) {
                Batch batch = transactional.createBatch();
                for (String query: queries) {
                  System.out.println(query);
                  batch.add(query);
                }
                batch.execute();
              }

              if (willPriceBeRefreshed[0]) {
                Long priceChangingLinkId = (isNowAvailable ? link.getId() : null); // in order to create a link_price history row
                CommonRepository.adjustProductPrice(transactional, link.getProductId(), link.getProductPrice(), priceChangingLinkId);
              }
              return true;
            });
          } catch (Exception e) {
            logger.error("Failed to handle status change", e);
          }

        }
      });

    });
  }

  public static void stop() {
    try {
      topic.removeAllListeners();
      tPool.shutdown();
      tPool.awaitTermination(SysProps.WAITING_TIME_FOR_TERMINATION(), TimeUnit.SECONDS);
    } catch (InterruptedException e) { }
  }

  private static String generateMakeAvailableQuery(Link link) {
    return
      String.format(
        "update link " + 
        "set sku='%s', name='%s', brand='%s', seller='%s', shipment='%s', price=%f, pre_status=status, status='%s', " +
        "site_id=%d, website_class_name='%s', retry=0, http_status=%d, problem=null, last_update=now() " +
        "where id=%d ",
        link.getSku(),
        link.getName(),
        link.getBrand(),
        link.getSeller(),
        link.getShipment(),
        link.getPrice(),
        link.getStatus().name(),
        link.getSiteId(),
        link.getWebsiteClassName(),
        link.getHttpStatus(),
        link.getId()
      );
  }

  private static String generateFailingQuery(Link link) {
    return
      String.format(
        "update link " + 
        "set retry=retry+1, http_status=%d, problem='%s', pre_status=status, status='%s', last_update=now() " +
        "where id=%d ",
        link.getHttpStatus(),
        link.getProblem().toUpperCase(),
        link.getStatus().name(),
        link.getId()
      );
  }

  private static String generateJustStatusUpdateQuery(Link link) {
    return
      String.format(
        "update link " + 
        "set pre_status=status, status='%s', last_update=now() " +
        "where id=%d ",
        link.getStatus().name(),
        link.getId()
      );
  }

  private static String generateLinkHistoryQuery(Link link) {
    String problemStatement = (link.getProblem() != null ? "'"+link.getProblem().toUpperCase()+"'" : null);
    return
      String.format(
        "insert into link_history (link_id, status, http_status, problem, product_id, company_id) values (%d, '%s', %d, %s, %d, %d) ",
        link.getId(),
        link.getStatus().name(),
        link.getHttpStatus(),
        problemStatement,
        link.getProductId(),
        link.getCompanyId()
      );
  }

  private static List<String> generateRefreshSpecListQueries(Link link) {
    List<String> list = new ArrayList<>();

    //deleting old specs
    list.add(
      String.format(
        "delete from link_spec where link_id=%d ", link.getId()
      )
    );

    // inserting new ones
    List<LinkSpec> specList = link.getSpecList();
    if (specList != null && specList.size() > 0) {
      for (LinkSpec spec: specList) {
        list.add(
          String.format(
            "insert into link_spec (link_id, _key, _value, product_id, company_id) values (%d, '%s', '%s', %d, %d)",
            link.getId(),
            spec.getKey(),
            spec.getValue(),
            link.getProductId(),
            link.getCompanyId()
          )
        );
      }
    }

    return list;
  }

}
