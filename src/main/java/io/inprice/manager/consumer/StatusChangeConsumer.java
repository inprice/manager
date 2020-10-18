package io.inprice.manager.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;

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
 * Consumer status changing messages sent from parser project.
 * This consumer handles two operations; status and price change.
 * 
 * @author mdpinar
 * @since 2020-10-18
 */
public class StatusChangeConsumer {

  private static final RTopic topic = RedisClient.createTopic(SysProps.REDIS_STATUS_CHANGE_TOPIC());
  
  public static void start() {
    ExecutorService tPool = Executors.newFixedThreadPool(Props.STATUS_CHANGE_CONSUMER_TPOOL_CAPACITY());

    topic.addListener(StatusChange.class, new MessageListener<StatusChange>() {
      public void onMessage(CharSequence channel, StatusChange change) {

        tPool.submit(new Runnable() {
          @Override
          public void run() {

            Link link = change.getLink();
            List<String> queries = new ArrayList<>();

            boolean isStatusChanged = (! change.getOldStatus().equals(link.getStatus()));
            boolean willPriceBeRefreshed = (! change.getOldPrice().equals(link.getPrice()));
            boolean isNowAvailable = (LinkStatus.AVAILABLE.equals(link.getStatus()));
            boolean wasPreviouslyAvailable = (LinkStatus.AVAILABLE.equals(link.getPreStatus()));
            boolean isFailing = LinkStatus.FAILED_GROUP.equals(link.getStatus().getGroup());

            if (isStatusChanged) {
              if (isNowAvailable) {
                queries.add(generateMakeAvailableQuery(link));
                queries.addAll(generateRefreshSpecListQueries(link));
              } else {
                if (isFailing) {
                  queries.add(generateFailingQuery(link));
                } else { // must be passive
                  queries.add(generateJustStatusUpdateQuery(link));
                }
                willPriceBeRefreshed = wasPreviouslyAvailable;
              }
              queries.add(generateLinkHistoryQuery(link));
            } 
            
            try (Handle handle = Database.getHandle()) {
              handle.inTransaction(transactional -> {
                if (queries.size() > 0) {
                  Batch batch = transactional.createBatch();
                  for (String query: queries) {
                    batch.add(query);
                  }
                  batch.execute();
                }

                if (willPriceBeRefreshed) {
                  Long priceChangingLinkId = (isNowAvailable ? link.getId() : null); // in order to create a link_price history row
                  CommonRepository.adjustProductPrice(transactional, link.getProductId(), link.getProductPrice(), priceChangingLinkId);
                }
                return true;
              });
            }

          }

        });
      };

    });

  }

  private static String generateMakeAvailableQuery(Link link) {
    return
      String.format(
        "update link " + 
        "set sku='%s', name='%s', brand='%s', seller='%s', shipment='%s', price=%d, pre_status=status, status='%s', " +
        "site_id=%d, website_class_name='%s', retry=0, http_status=0, problem=null, last_update=now() " +
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
        link.getProblem(),
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
    return
      String.format(
        "insert into link_history (link_id, status, http_status, problem, product_id, company_id) values (%d, '%s', %d, '%s', %d, %d) ",
        link.getId(),
        link.getStatus().name(),
        link.getHttpStatus(),
        link.getProblem(),
        link.getProductId(),
        link.getCompanyId()
      );
  }

  private static List<String> generateRefreshSpecListQueries(Link link) {
    List<String> list = new ArrayList<>(link.getSpecList().size()+1);

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



if (price) {
  commonDao.insertLinkPrice(
    pl.getId(),
    price,
    position,
    pl.getProductId(),
    pl.getCompanyId()
  );
}
