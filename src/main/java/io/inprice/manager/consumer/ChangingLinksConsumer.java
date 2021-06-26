package io.inprice.manager.consumer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
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
import io.inprice.common.converters.GroupRefreshResultConverter;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.GroupRefreshResult;
import io.inprice.common.info.LinkStatusChange;
import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.meta.LinkStatusGroup;
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkGroup;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.repository.AlarmDao;
import io.inprice.common.repository.CommonDao;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.helpers.RedisClient;

/**
 * This is a the most important class to mange links' states, prices and alarms.
 * 
 * @author mdpinar
 * @since 2020-10-18
 */
public class ChangingLinksConsumer {

  private static final Logger logger = LoggerFactory.getLogger(ChangingLinksConsumer.class);

  private static RTopic topic;
  private static ExecutorService tPool;

  public static void start() {
  	tPool = Executors.newFixedThreadPool(SysProps.TPOOL_LINK_CONSUMER_CAPACITY);

  	topic = RedisClient.createTopic(SysProps.REDIS_STATUS_CHANGE_TOPIC);
    topic.addListener(LinkStatusChange.class, (channel, change) -> {

      tPool.submit(new Runnable() {

        @Override
        public void run() {
          Link link = change.getLink();
          List<String> queries = new ArrayList<>();
          
          final LinkStatus newStatus = link.getStatus();
          final LinkStatus oldStatus = change.getOldStatus();

          boolean isStatusChanged = false;
          boolean[] willPriceBeRefreshed = { false };

          //if the link is now available
          if (newStatus.equals(LinkStatus.AVAILABLE)) {
          	if (oldStatus.equals(newStatus)) { //if it is previously available then check its price if there is a change
          		willPriceBeRefreshed[0] = (link.getPrice().doubleValue() != change.getOldPrice().doubleValue());
          	} else {
              queries.add(queryMakeAvailable(link));
            	queries.addAll(queryRefreshSpecList(link));
      				isStatusChanged = true;
      				willPriceBeRefreshed[0] = true;
          	}
          }
          
          boolean willBeNonActive = false;

          //if it fails 
          if (LinkStatusGroup.TRYING.equals(newStatus.getGroup())) {

          	if (oldStatus.equals(newStatus) && link.getRetry() < 3) {
              queries.add(queryIncreaseRetry(link));
            } else {
              if (! oldStatus.equals(newStatus)) {
              	willBeNonActive = true;
        			} else {
        				logger.warn("Link with id {} is in wrong state! New Status: {}, Old Status: {}, Retry: {} ", 
        						link.getId(), change.getOldStatus(), link.getStatus(), link.getRetry());
            	}
      			}
          }

          //if it is now passive then lets terminate it, no need to retry
          if (LinkStatusGroup.PROBLEM.equals(newStatus.getGroup())) {
          	willBeNonActive = true;
          }

        	if (willBeNonActive) {
          	isStatusChanged = true;
      			queries.add(queryMakeLinkNonActive(link));
      			willPriceBeRefreshed[0] = oldStatus.equals(LinkStatus.AVAILABLE);
          }

          if (isStatusChanged) {
            queries.add(queryInsertLinkHistory(link));
          }

          //check if link is alarmed and conditions are suitable to fire an alarm.
          if (link.getAlarm() != null && (isStatusChanged || willPriceBeRefreshed[0])) {
          	String alarmUpdatingQuery = checkAndGenerateUpdateQueryForLinkAlarm(change, isStatusChanged, willPriceBeRefreshed[0]);
          	if (alarmUpdatingQuery != null) {
          		queries.add(alarmUpdatingQuery);
          	}
          }

          try (Handle handle = Database.getHandle()) {
          	handle.begin();

            if (queries.size() > 0) {
              Batch batch = handle.createBatch();
              for (String query: queries) {
                batch.add(query);
              }
              batch.execute();
            }

            if (willPriceBeRefreshed[0]) {
            	CommonDao commonDao = handle.attach(CommonDao.class);
        			GroupRefreshResult grr = GroupRefreshResultConverter.convert(commonDao.refreshGroup(link.getGroupId()));

        			//check if group is alarmed and conditions are suitable for firing an alarm.
        			if (grr.getAlarmId() != null) {
        				AlarmDao alarmDao = handle.attach(AlarmDao.class);
        				LinkGroup group = alarmDao.findGroupAndAlarmById(link.getGroupId());
        				if (group != null) {
                	String alarmUpdatingQuery = checkAndGenerateUpdateQueryForGroupAlarm(group);
                	if (alarmUpdatingQuery != null) {
                		handle.execute(alarmUpdatingQuery);
                	}
        				}
        			}

            	BigDecimal diffAmount = BigDecimal.ZERO;
            	BigDecimal diffRate = BigDecimal.ZERO;
            	if (change.getOldPrice() != null && change.getOldPrice().compareTo(BigDecimal.ZERO) > 0) {
              	diffAmount = link.getPrice().subtract(change.getOldPrice()).setScale(2, RoundingMode.HALF_UP);
              	diffRate = link.getPrice().divide(change.getOldPrice()).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
            	}
            	commonDao.insertLinkPrice(link.getId(), link.getPrice(), diffAmount, diffRate, link.getGroupId(), link.getAccountId());
            }

            if (queries.size() > 0)
            	handle.commit();
            else
            	handle.rollback();

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
      tPool.awaitTermination(SysProps.WAITING_TIME_FOR_TERMINATION, TimeUnit.SECONDS);
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
    return
      String.format(
        "insert into link_history (link_id, status, http_status, group_id, account_id) values (%d, '%s', %d, %d, %d) ",
        link.getId(),
        link.getStatus(),
        link.getHttpStatus(),
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

  private static String checkAndGenerateUpdateQueryForGroupAlarm(LinkGroup group) {
  	boolean willBeUpdated = false;
  	
  	Alarm alarm = group.getAlarm();

  	if (AlarmSubject.STATUS.equals(alarm.getSubject())) {
  		switch (alarm.getSubjectWhen()) {
  			case EQUAL: {
  				willBeUpdated = group.getLevel().name().equals(alarm.getCertainStatus());
  				break;
  			}
  			case NOT_EQUAL: {
  				willBeUpdated = !group.getLevel().name().equals(alarm.getCertainStatus());
  				break;
  			}
  			default: {
  				willBeUpdated = true;
  				break;
  			}
			}
  	}

  	BigDecimal newAmount = null;

  	if (! AlarmSubject.STATUS.equals(alarm.getSubject())) {
  		switch (alarm.getSubject()) {
  			case MINIMUM: {
  				newAmount = group.getMinPrice();
  				break;
  			}
  			case AVERAGE: {
  				newAmount = group.getAvgPrice();
  				break;
  			}
  			case MAXIMUM: {
  				newAmount = group.getMaxPrice();
  				break;
  			}
  			case TOTAL: {
  				newAmount = group.getTotal();
  				break;
  			}
				default: break;
			}

  		if (newAmount != null) {
    		switch (alarm.getSubjectWhen()) {
    			case INCREASED: {
    				willBeUpdated = newAmount.compareTo(alarm.getLastAmount()) > 0;
    				break;
    			}
    			case DECREASED: {
    				willBeUpdated = newAmount.compareTo(alarm.getLastAmount()) < 0;
    				break;
    			}
    			case OUT_OF_LIMITS: {
  					willBeUpdated = checkIfPriceIsOutOfLimits(newAmount, alarm.getAmountLowerLimit(), alarm.getAmountUpperLimit());
    				break;
    			}
    			default: {
    				willBeUpdated = true;
    				break;
    			}
    		}
  		}
  	}

  	if (willBeUpdated) {
  		String tobeNotifiedPart = "tobe_notified=true, ";

    	//checks if it is already notified within 5 mins. if so, no need to disturb the customer!
    	if (willBeUpdated && alarm.getNotifiedAt() != null) {
        long diff = DateUtils.findMinuteDiff(alarm.getNotifiedAt(), new Date());
        if (diff <= 5) {
        	tobeNotifiedPart = "";
        }
    	}

  		return
        String.format(
          "update alarm set last_status='%s', last_amount=%f, %s updated_at=now() where id=%d ",
          group.getLevel(),
          newAmount,
          tobeNotifiedPart,
          group.getAlarmId()
        );
  	}

  	return null;
  }

  private static String checkAndGenerateUpdateQueryForLinkAlarm(LinkStatusChange change, boolean isStatusChanged, boolean isPriceChanged) {
  	boolean willBeUpdated = false;

  	Link link = change.getLink();
  	Alarm alarm = link.getAlarm();

  	if (isStatusChanged && AlarmSubject.STATUS.equals(alarm.getSubject())) {
  		switch (alarm.getSubjectWhen()) {
  			case EQUAL: {
  				willBeUpdated = link.getStatus().name().equals(alarm.getCertainStatus());
  				break;
  			}
  			case NOT_EQUAL: {
  				willBeUpdated = !link.getStatus().name().equals(alarm.getCertainStatus());
  				break;
  			}
  			default: {
  				willBeUpdated = true;
  				break;
  			}
			}
  	}
  	
  	if (isPriceChanged && AlarmSubject.PRICE.equals(alarm.getSubject())) {
  		switch (alarm.getSubjectWhen()) {
  			case INCREASED: {
  				willBeUpdated = link.getPrice().compareTo(alarm.getLastAmount()) > 0;
  				break;
  			}
  			case DECREASED: {
  				willBeUpdated = link.getPrice().compareTo(alarm.getLastAmount()) < 0;
  				break;
  			}
  			case OUT_OF_LIMITS: {
  				willBeUpdated = checkIfPriceIsOutOfLimits(link.getPrice(), alarm.getAmountLowerLimit(), alarm.getAmountUpperLimit());
  				break;
  			}
  			default: {
  				willBeUpdated = true;
  				break;
  			}
  		}
  	}

  	if (willBeUpdated) {
  		String tobeNotifiedPart = "tobe_notified=true, ";

    	//checks if it is already notified within 5 mins. if so, no need to disturb the customer!
    	if (willBeUpdated && alarm.getNotifiedAt() != null) {
        long diff = DateUtils.findMinuteDiff(alarm.getNotifiedAt(), new Date());
        if (diff <= 5) {
        	tobeNotifiedPart = "";
        }
    	}

      return
        String.format(
          "update alarm set last_status='%s', last_amount=%f, %s updated_at=now() where id=%d ",
          link.getStatus(),
          link.getPrice(),
          tobeNotifiedPart,
          link.getAlarmId()
        );
  	}

  	return null;
  }

  private static boolean checkIfPriceIsOutOfLimits(BigDecimal price, BigDecimal lowerLimit, BigDecimal upperLimit) {
  	if (price.compareTo(BigDecimal.ZERO) > 0) {
  		if (lowerLimit.compareTo(BigDecimal.ZERO) > 0) {
  			boolean yes = price.compareTo(lowerLimit) < 0;
  			if (yes) return true;
  		}
    	if (upperLimit.compareTo(BigDecimal.ZERO) > 0) {
  			boolean yes = price.compareTo(upperLimit) > 0;
  			if (yes) return true;
  		}
  	}
		return false;
  }
  
}
