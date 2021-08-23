package io.inprice.manager.consumer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.inprice.common.config.QueueDef;
import io.inprice.common.converters.GroupRefreshResultConverter;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.GroupRefreshResult;
import io.inprice.common.info.LinkStatusChange;
import io.inprice.common.info.ParseStatus;
import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkGroup;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.repository.AlarmDao;
import io.inprice.common.repository.CommonDao;
import io.inprice.common.utils.DateUtils;
import io.inprice.common.utils.StringUtils;

/**
 * This is the most important class to mange links' states, prices and alarms.
 * 
 * @author mdpinar
 * @since 2020-10-18
 */
class StatusChangingLinksConsumer {

  private static final Logger logger = LoggerFactory.getLogger(StatusChangingLinksConsumer.class);

  StatusChangingLinksConsumer(QueueDef queueDef) throws IOException {
  	String forWhichConsumer = "MAN-CON: " + queueDef.NAME;

  	Connection conn = RabbitMQ.createConnection(forWhichConsumer, queueDef.CAPACITY);
		Channel channel = conn.createChannel();

		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
	      try {
	      	LinkStatusChange change = JsonConverter.fromJson(new String(body), LinkStatusChange.class);

          List<String> queries = new ArrayList<>();
          boolean hasStatusChanged = false;
          boolean willPriceBeRefreshed = false;
          boolean willBecomeNonActive = false;

          LinkStatus oldStatus = change.getLink().getStatus();
          LinkStatus newStatus = findNewStatusByParseCode(change.getLink());

					if (change.getLink().getParseProblem() != null) {
						change.getLink().setParseProblem(StringUtils.clearErrorMessage(change.getLink().getParseProblem()));
					}

          switch (newStatus.getGroup()) {
          	case ACTIVE: {
	          	if (oldStatus.equals(newStatus)) {
	          		willPriceBeRefreshed = (change.getLink().getPrice().doubleValue() != change.getOldPrice().doubleValue());
	          	} else {
	              queries.add(queryMakeAvailable(change.getLink()));
	            	queries.addAll(queryRefreshSpecList(change.getLink()));
	      				hasStatusChanged = true;
	      				willPriceBeRefreshed = true;
	          	}
							break;
						}

          	case TRYING: {
            	if (change.getLink().getRetry() < 3) {
                queries.add(queryIncreaseRetry(change.getLink()));
              } else {
              	willBecomeNonActive = true;
        			}
							break;
						}

          	case WAITING: {
          		hasStatusChanged = (oldStatus.equals(newStatus) == false);
          		willPriceBeRefreshed = (oldStatus.equals(LinkStatus.AVAILABLE));
							break;
						}

          	case PROBLEM: {
            	willBecomeNonActive = true;
							break;
						}
					}

        	if (willBecomeNonActive) {
          	hasStatusChanged = true;
      			queries.add(queryMakeLinkNonActive(change.getLink()));
      			willPriceBeRefreshed = oldStatus.equals(LinkStatus.AVAILABLE);
          }

          if (hasStatusChanged) {
            queries.add(queryInsertLinkHistory(change.getLink()));
          }

          //check if link is alarmed and conditions are suitable for firing an alarm.
          if (change.getLink().getAlarm() != null && (hasStatusChanged || willPriceBeRefreshed)) {
          	String alarmUpdatingQuery = checkAndGenerateUpdateQueryForLinkAlarm(change, hasStatusChanged, willPriceBeRefreshed);
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

            if (willPriceBeRefreshed) {
            	CommonDao commonDao = handle.attach(CommonDao.class);
        			GroupRefreshResult grr = GroupRefreshResultConverter.convert(commonDao.refreshGroup(change.getLink().getGroupId()));

        			//check if group is alarmed and conditions are suitable for firing an alarm.
        			if (grr.getAlarmId() != null) {
        				AlarmDao alarmDao = handle.attach(AlarmDao.class);
        				LinkGroup group = alarmDao.findGroupAndAlarmById(change.getLink().getGroupId());
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
              	diffAmount = change.getLink().getPrice().subtract(change.getOldPrice()).setScale(2, RoundingMode.HALF_UP);
              	diffRate = change.getLink().getPrice().divide(change.getOldPrice()).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
            	}
            	commonDao.insertLinkPrice(change.getLink().getId(), change.getLink().getPrice(), diffAmount, diffRate, change.getLink().getGroupId(), change.getLink().getAccountId());
            }

            if (queries.size() > 0)
            	handle.commit();
            else
            	handle.rollback();
          }	
        } catch (Exception e) {
          logger.error("Failed to handle status change", e);
        }
      }
		};

		logger.info(forWhichConsumer + " is up and running.");
		channel.basicConsume(queueDef.NAME, true, consumer);
  }
  
  private static LinkStatus findNewStatusByParseCode(Link link) {
    switch (link.getParseCode()) {
    	case ParseStatus.CODE_OK: return LinkStatus.AVAILABLE;
    	case ParseStatus.CODE_NO_DATA: return LinkStatus.NO_DATA;
    	case ParseStatus.CODE_NOT_AVAILABLE: return LinkStatus.NOT_AVAILABLE;
    	case ParseStatus.CODE_NOT_IMPLEMENTED: return LinkStatus.TOBE_IMPLEMENTED;
  		/*------------------------------------------*/
    	case ParseStatus.CODE_IO_EXCEPTION: return LinkStatus.NETWORK_ERROR;
    	case ParseStatus.CODE_TIMEOUT_EXCEPTION: return LinkStatus.TIMED_OUT;
    	case ParseStatus.CODE_UNEXPECTED_EXCEPTION: return LinkStatus.INTERNAL_ERROR;
  		/*------------------------------------------*/
			case ParseStatus.HTTP_CODE_NOT_FOUND: return LinkStatus.NOT_FOUND;
    	case ParseStatus.HTTP_CODE_NOT_ALLOWED: return LinkStatus.NOT_ALLOWED;
			case ParseStatus.HTTP_CODE_UNREACHABLE_SITE: return LinkStatus.SITE_DOWN;
  		/*------------------------------------------*/
			default: {
				if (link.getParseCode() >= 400) { //http error
					return LinkStatus.NETWORK_ERROR;
				} else {
					return link.getStatus();
				}
			}
    }
  }

  private static String queryMakeAvailable(Link link) {
    return
      String.format(
        "update link " + 
        "set sku='%s', name='%s', brand='%s', seller='%s', shipment='%s', price=%f, pre_status=status, status='%s', status_group='%s', " +
        "platform_id=%d, retry=0, parse_code=0, parse_problem=null, updated_at=now() " +
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
        link.getId()
      );
  }

  private static String queryIncreaseRetry(Link link) {
    return
      String.format(
        "update link " + 
        "set retry=retry+1, parse_code=%d, parse_problem='%s', updated_at=now() " +
        "where id=%d ",
        link.getParseCode(),
        link.getParseProblem(),
        link.getId()
      );
  }

  private static String queryMakeLinkNonActive(Link link) {
  	return
			String.format(
				"update link " + 
					"set retry=0, parse_code=%d, parse_problem='%s', pre_status=status, status='%s', status_group='%s', updated_at=now(), " + 
					" platform_id= " + (link.getPlatformId() != null ? link.getPlatformId() : "null") +
					" where id=%d ",
					link.getParseCode(),
					link.getParseProblem(),
					link.getStatus(),
					link.getStatus().getGroup(),
					link.getId()
				);
  }

  private static String queryInsertLinkHistory(Link link) {
    return
      String.format(
        "insert into link_history (link_id, status, parse_code, group_id, account_id) values (%d, '%s', %d, %d, %d) ",
        link.getId(),
        link.getStatus(),
        link.getParseCode(),
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
    if (CollectionUtils.isNotEmpty(link.getSpecList())) {
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
