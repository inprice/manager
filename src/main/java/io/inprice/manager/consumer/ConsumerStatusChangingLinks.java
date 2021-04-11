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
import io.inprice.common.meta.LinkStatusGroup;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.repository.CommonRepository;
import io.inprice.manager.helpers.RedisClient;

/**
 * Handles status change and price change
 * 
 * @author mdpinar
 * @since 2020-10-18
 */
public class ConsumerStatusChangingLinks {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerStatusChangingLinks.class);

  private static RTopic topic;

  //since parallel status change operations for the links under one group can cause fields
  //to be miscalculated such as avg and mac prices
  //this thread pool must be the capacity of 1
  private static final ExecutorService tPool = Executors.newFixedThreadPool(1);
  
  public static void start() {
  	topic = RedisClient.createTopic(SysProps.REDIS_STATUS_CHANGE_TOPIC());
    topic.addListener(StatusChange.class, (channel, change) -> {

      tPool.submit(new Runnable() {

      	@Override
        public void run() {
          Link link = change.getLink();
          List<String> queries = new ArrayList<>();
          
          final LinkStatus newStatus = link.getStatus();
          final LinkStatus oldStatus = change.getOldStatus();

          boolean isNowAvailable = (LinkStatus.AVAILABLE.equals(link.getStatus()));

          boolean isInsertHistory = false;
          boolean[] isPriceRefresh = { false };

          //if the link is now available
          if (newStatus.equals(LinkStatus.AVAILABLE)) {
          	if (oldStatus.equals(newStatus)) { //if it is previously available then check its price if there is a change
          		isPriceRefresh[0] = (link.getPrice().doubleValue() != change.getOldPrice().doubleValue());
          	} else {
              queries.add(queryMakeAvailable(link));
            	queries.addAll(queryRefreshSpecList(link));
      				isInsertHistory = true;
      				isPriceRefresh[0] = true;
          	}
          }
          
          //if it fails 
          if (LinkStatusGroup.TRYING.equals(newStatus.getGroup())) {
          	if (link.getRetry() < 3) {
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
          if (LinkStatusGroup.PROBLEM.equals(newStatus.getGroup())) {
          	isInsertHistory = true;
      			queries.add(queryMakeLinkNonActive(link));
      			isPriceRefresh[0] = oldStatus.equals(LinkStatus.AVAILABLE);
          }

          if (isInsertHistory) {
            queries.add(queryInsertLinkHistory(link));
          }

          try (Handle handle = Database.getHandle()) {
            handle.inTransaction(transaction -> {
              if (queries.size() > 0) {
                Batch batch = transaction.createBatch();
                for (String query: queries) {
                  batch.add(query);
                }
                batch.execute();
              }

              if (isPriceRefresh[0]) {
                Long priceChangingLinkId = (isNowAvailable ? link.getId() : null); // in order to add a link_price history row
                CommonRepository.refreshGroup(transaction, link.getGroupId(), priceChangingLinkId);
              }
              return (queries.size() > 0);
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
        "set sku='%s', name='%s', brand='%s', seller='%s', shipment='%s', price=%f, pre_status=status, status='%s', status_group='%s', " +
        "platform_id=%d, retry=0, http_status=%d, problem=null, updated_at=now() " +
        "where id=%d ",
        link.getSku(),
        link.getName(),
        link.getBrand(),
        link.getSeller(),
        link.getShipment(),
        link.getPrice(),
        link.getStatus(),
        link.getStatus().getGroup(),
        link.getPlatformId(),
        link.getHttpStatus(),
        link.getId()
      );
  }

  private static String queryIncreaseRetry(Link link) {
    return
      String.format(
        "update link " + 
        "set retry=retry+1, problem='%s', http_status=%d, updated_at=now() " +
        "where id=%d ",
        link.getProblem(),
        link.getHttpStatus(),
        link.getId()
      );
  }

  private static String queryMakeLinkNonActive(Link link) {
  	return
			String.format(
				"update link " + 
					"set retry=0, http_status=%d, problem='%s', pre_status=status, status='%s', status_group='%s', updated_at=now(), " + 
					" platform_id= " + (link.getPlatformId() != null ? link.getPlatformId() : "null") +
					" where id=%d ",
					link.getHttpStatus(),
					link.getProblem(),
					link.getStatus(),
					link.getStatus().getGroup(),
					link.getId()
				);
  }

  private static String queryInsertLinkHistory(Link link) {
    String problemStatement = (link.getProblem() != null ? "'"+link.getProblem().toUpperCase()+"'" : null);
    return
      String.format(
        "insert into link_history (link_id, status, http_status, problem, group_id, account_id) values (%d, '%s', %d, %s, %d, %d) ",
        link.getId(),
        link.getStatus(),
        link.getHttpStatus(),
        problemStatement,
        link.getGroupId(),
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
            "insert into link_spec (link_id, _key, _value, group_id, account_id) values (%d, '%s', '%s', %d, %d)",
            link.getId(),
            spec.getKey(),
            spec.getValue(),
            link.getGroupId(),
            link.getAccountId()
          )
        );
      }
    }

    return list;
  }

}
