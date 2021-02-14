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
import io.inprice.manager.helpers.RedisClient;

/**
 * Handles status change, price change and imported products via link records
 * 
 * @author mdpinar
 * @since 2020-10-18
 */
public class StatusChangingLinksConsumer {

  private static final Logger logger = LoggerFactory.getLogger(StatusChangingLinksConsumer.class);

  private static final RTopic topic = RedisClient.createTopic(SysProps.REDIS_STATUS_CHANGE_TOPIC());

  //since parallel status change operations for the links under one product can cause fields
  //to be miscalculated such as avg and mac prices
  //this thread pool must be the capacity of 1
  private static final ExecutorService tPool = Executors.newFixedThreadPool(1);
  
  public static void start() {
    topic.addListener(StatusChange.class, (channel, change) -> {

      tPool.submit(new Runnable() {
        @Override
        public void run() {

          Link link = change.getLink();
          List<String> queries = new ArrayList<>();
          
          final LinkStatus newStatus = link.getStatus();
          final LinkStatus oldStatus = change.getOldStatus();

          boolean isImportedProduct = (link.getImportDetailId() != null);
          boolean isNowAvailable = (LinkStatus.AVAILABLE.equals(link.getStatus()));

          boolean isInsertHistory = false;
          boolean[] isPriceRefresh = { false };

          //if the link is now available
          if (newStatus.equals(LinkStatus.AVAILABLE)) {
          	if (oldStatus.equals(newStatus)) { //if it is previously available then check its price if there is a change
          		isPriceRefresh[0] = (link.getPrice().doubleValue() != change.getOldPrice().doubleValue());
          	} else {
              if (isImportedProduct) { //if importing link
                queries.addAll(queryCreateProductViaLink(link));
              } else { // if is a normal link
                queries.add(queryMakeAvailable(link));
              	queries.addAll(queryRefreshSpecList(link));
        				isInsertHistory = true;
        				isPriceRefresh[0] = true;
              }
          	}
          }
          
          //if it fails 
          if (newStatus.getGroup().equals(LinkStatus.FAILED_GROUP)) {
          	if (link.getRetry() < 3) {
            	if (isImportedProduct) {
            		queries.add(queryUpdateImportDetailLastCheck(link));
              }
              queries.add(queryIncreaseRetry(link));
            } else {
              if (! oldStatus.equals(newStatus)) {
              	queries.add(queryMakeLinkNonActive(link));
        				isInsertHistory = true;
        				isPriceRefresh[0] = oldStatus.equals(LinkStatus.AVAILABLE);
        			} else {
        				logger.warn("Link with id {} is in wrong state! New Status: {}, Old Status: {}, Retry: {} ", 
        						link.getId(), change.getOldStatus(), link.getStatus(), link.getRetry());
            	}
      			}
          }

          //if it is now passive then lets terminate it, no need to retry
          if (newStatus.getGroup().equals(LinkStatus.PASSIVE_GROUP)) {
      			queries.add(queryMakeLinkNonActive(link));
          	if (isImportedProduct) {
          		queries.add(queryMakeImportDetailNonActive(link));
            } else {
        			isInsertHistory = true;
        			isPriceRefresh[0] = oldStatus.equals(LinkStatus.AVAILABLE);
            }
          }

          if (isInsertHistory) {
            queries.add(queryInsertLinkHistory(link));
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

              if (isPriceRefresh[0]) {
                Long priceChangingLinkId = (isNowAvailable ? link.getId() : null); // in order to add a link_price history row
                CommonRepository.adjustProductPrice(transactional, link.getProductId(), priceChangingLinkId);
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

  private static String queryMakeAvailable(Link link) {
    return
      String.format(
        "update link " + 
        "set sku='%s', name='%s', brand='%s', seller='%s', shipment='%s', price=%f, pre_status=status, status='%s', " +
        "platform_id=%d, retry=0, http_status=%d, problem=null, last_update=now() " +
        "where id=%d ",
        link.getSku(),
        link.getName(),
        link.getBrand(),
        link.getSeller(),
        link.getShipment(),
        link.getPrice(),
        link.getStatus().name(),
        link.getPlatformId(),
        link.getHttpStatus(),
        link.getId()
      );
  }

  private static String queryIncreaseRetry(Link link) {
    return
      String.format(
        "update link " + 
        "set retry=retry+1, problem='%s', http_status=%d, last_update=now() " +
        "where id=%d ",
        link.getProblem(),
        link.getHttpStatus(),
        link.getId()
      );
  }

  private static String queryUpdateImportDetailLastCheck(Link link) {
    return
      "update import_detail " + 
      "set imported=false, last_check=now() " + 
      (link.getProblem() != null ? ", status='"+link.getProblem()+"'" : "") +
      " where id=" + link.getImportDetailId();
  }

  private static List<String> queryCreateProductViaLink(Link link) {
    List<String> list = new ArrayList<>(4);

    link.setStatus(LinkStatus.IMPORTED);
    link.setProblem("");
    link.setHttpStatus(200);
    list.add(queryUpdateStatus(link));

    list.add("update import_detail set imported=true, status='IMPORTED', last_check=now() where id=" + link.getImportDetailId());

    // before this is resolved, user may add a product with the same code. let's be cautious!
    list.add(
      String.format(
        "insert into product (code, name, price, account_id) " +
        "select * from " +
        "(select '%s' as code, '%s' as name, %f as price, %d as account_id) as tmp " +
        "  where not exists ( " +
        "    select code from product where code = '%s' " +
        ") limit 1 ",
        link.getSku(),
        link.getName(),
        link.getPrice(),
        link.getAccountId(),
        link.getSku()
      )
    );

    list.add("update account set product_count=product_count+1 where id=" + link.getAccountId());

    return list;
  }
  
  private static String queryMakeLinkNonActive(Link link) {
  	return
  			String.format(
  					"update link " + 
  							"set retry=0, http_status=%d, problem='%s', pre_status=status, status='%s', last_update=now() " +
  							"where id=%d ",
  							link.getHttpStatus(),
  							link.getProblem(),
  							link.getStatus().name(),
  							link.getId()
  					);
  }

  private static String queryMakeImportDetailNonActive(Link link) {
    return
      String.format(
        "update import_detail " + 
        "set status='%s', last_check=now() " +
        "where id=%d ",
        link.getProblem(),
        link.getImportDetailId()
      );
  }

  private static String queryUpdateStatus(Link link) {
    return
      String.format(
        "update link " + 
        "set retry=0, http_status=%d, problem='%s', pre_status=status, status='%s', last_update=now() " +
        "where id=%d ",
        link.getHttpStatus(),
        link.getProblem(),
        link.getStatus().name(),
        link.getId()
      );
  }

  private static String queryInsertLinkHistory(Link link) {
    String problemStatement = (link.getProblem() != null ? "'"+link.getProblem().toUpperCase()+"'" : null);
    return
      String.format(
        "insert into link_history (link_id, status, http_status, problem, product_id, account_id) values (%d, '%s', %d, %s, %d, %d) ",
        link.getId(),
        link.getStatus().name(),
        link.getHttpStatus(),
        problemStatement,
        link.getProductId(),
        link.getAccountId()
      );
  }

  private static List<String> queryRefreshSpecList(Link link) {
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
            "insert into link_spec (link_id, _key, _value, product_id, account_id) values (%d, '%s', '%s', %d, %d)",
            link.getId(),
            spec.getKey(),
            spec.getValue(),
            link.getProductId(),
            link.getAccountId()
          )
        );
      }
    }

    return list;
  }

}
