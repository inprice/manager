package io.inprice.manager.consumer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import io.inprice.common.converters.ProductRefreshResultConverter;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.ProductRefreshResult;
import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.meta.Grup;
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Link;
import io.inprice.common.models.Product;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.repository.AlarmDao;
import io.inprice.common.repository.CommonDao;
import io.inprice.common.utils.DateUtils;

/**
 * This is the most important class to mange links' statuses, prices and alarms.
 *
 * The main reason for the complexity in sql clauses here is
 * each update/insert/delete for a link is made for all the other links having the same url so that no need to handle the same urls again and again
 * 
 * @author mdpinar
 * @since 2020-10-18
 */
class StatusChangingLinksConsumer {

  private static final Logger logger = LoggerFactory.getLogger(StatusChangingLinksConsumer.class);
  private static final BigDecimal A_HUNDRED = new BigDecimal(100);

  StatusChangingLinksConsumer(QueueDef queueDef) throws IOException {
  	String forWhichConsumer = "MAN-CON: " + queueDef.NAME;

  	Connection conn = RabbitMQ.createConnection(forWhichConsumer);
		Channel channel = conn.createChannel();

		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
	      try {
	      	Link linkFromParser = JsonConverter.fromJsonWithoutJsonIgnore(new String(body), Link.class);

          try (Handle handle = Database.getHandle()) {
          	handle.begin();

          	CommonDao commonDao = handle.attach(CommonDao.class);
          	AlarmDao alarmDao = handle.attach(AlarmDao.class);

          	Set<Long> alarmedProducts = new HashSet<>();
          	Set<Long> alarmedLinks = new HashSet<>();

	        	List<Link> linksFromDb = commonDao.findActiveLinksByHash(linkFromParser.getUrlHash());

	        	if (CollectionUtils.isNotEmpty(linksFromDb)) {
	        		for (Link linkFromDb: linksFromDb) {

			          boolean hasStatusChanged = (linkFromParser.getStatus().equals(linkFromDb.getStatus()) == false);

			          boolean willPriceBeRefreshed = false;
			          if (hasStatusChanged) {
			          	willPriceBeRefreshed = (linkFromDb.getStatus().equals(LinkStatus.AVAILABLE) || linkFromParser.getStatus().equals(LinkStatus.AVAILABLE));
			          } else {
			          	willPriceBeRefreshed = (linkFromParser.getStatus().equals(LinkStatus.AVAILABLE) && linkFromParser.getPrice().compareTo(linkFromDb.getPrice()) != 0);
			          }

			          List<String> queries = new ArrayList<>();

			          switch (linkFromParser.getStatus().getGrup()) {
			          	case ACTIVE: {
				          	if (hasStatusChanged) {
				              queries.add(queryMakeAvailable(linkFromDb.getId(), linkFromParser));
				            	queries.addAll(queryRefreshSpecList(linkFromDb, linkFromParser));
				          	} else if (willPriceBeRefreshed) {
				          		queries.add(queryUpdatePrice(linkFromDb.getId(), linkFromParser.getPrice(), linkFromParser.getPrice().compareTo(linkFromDb.getPrice())));
				          	} else if (linkFromDb.getRetry() > 0 || linkFromDb.getParseCode().equals(linkFromParser.getParseCode()) == false) {
				          		queries.add(queryClearActiveLink(linkFromDb.getId()));
				          	}
										break;
									}
			
			          	case TRYING: {
			          		willPriceBeRefreshed = false;
			          		linkFromDb.setRetry(linkFromDb.getRetry()+1);
		            		if (linkFromDb.getRetry() < 3) {
			            		hasStatusChanged = false;
			                queries.add(queryIncreaseRetry(linkFromDb, linkFromParser));
			        			}
										break;
									}
			
			          	case WAITING:
			          	case PROBLEM: {
			          		break;
			          	}
								}
			
			        	if (hasStatusChanged) {
			        		if (linkFromParser.getStatus().getGrup().equals(Grup.ACTIVE) == false) { //already handled above
			        			queries.add(queryUpdateLinkStatus(linkFromParser));
			        		}
			      			queries.add(queryAddStatusHistory(linkFromDb, linkFromParser));
			          }
		        	
		          	if (queries.size() > 0) {
		              Batch batch = handle.createBatch();
		              for (String query: queries) {
		                batch.add(query);
		              }
		              batch.execute();
		            }

		            //check if link is alarmed and conditions are suitable for firing an alarm.
		            if (hasStatusChanged && alarmedLinks.contains(linkFromDb.getId()) == false) {
	            		alarmedLinks.add(linkFromDb.getId());

	            		if (linkFromDb.getAlarmId() != null) {
			            	String alarmUpdatingQuery = checkAndGenerateUpdateQueryForLinkAlarm(linkFromDb, linkFromParser);
			            	if (alarmUpdatingQuery != null) {
			            		handle.execute(alarmUpdatingQuery);
			            	}
	            		}
		        		}

		        		//if price is changed then we need to calculate diff to insert price history
		        		//and also, there may an alarm on the products of those links sensitive for changings like min/max/avg
		            if (willPriceBeRefreshed) {
	              	BigDecimal diffAmount = BigDecimal.ZERO;
	              	BigDecimal diffRate = BigDecimal.ZERO;
	
	              	if (linkFromDb.getPrice().compareTo(BigDecimal.ZERO) != 0) {
	  	            	diffAmount = linkFromParser.getPrice().subtract(linkFromDb.getPrice()).setScale(2, RoundingMode.HALF_UP);
	  	            	if (diffAmount.compareTo(BigDecimal.ZERO) != 0) {
	  	            		diffRate = diffAmount.divide(linkFromDb.getPrice(), 6, RoundingMode.HALF_UP).multiply(A_HUNDRED).setScale(2, RoundingMode.HALF_UP);
		            		}
	              	}

	              	if (diffAmount.compareTo(BigDecimal.ZERO) != 0) {
	              		commonDao.insertLinkPrice(linkFromDb.getId(), linkFromDb.getPrice(), linkFromParser.getPrice(), diffAmount, diffRate, linkFromDb.getProductId(), linkFromDb.getWorkspaceId());
	              	}

	              	//product alarms
	              	//to prevent redundant modifications and alarms for the same product
	            		if (alarmedProducts.contains(linkFromDb.getProductId()) == false) {
		            		alarmedProducts.add(linkFromDb.getProductId());
		
		            		//check if product is alarmed and conditions are suitable for firing an alarm.
		          			ProductRefreshResult PRR = ProductRefreshResultConverter.convert(commonDao.refreshProduct(linkFromDb.getProductId()));
		          			if (PRR.getAlarmId() != null) {
			        				Product product = alarmDao.findProductAndAlarmById(linkFromDb.getProductId());
			        				if (product != null) {
			                	String alarmUpdatingQuery = checkAndGenerateUpdateQueryForProductAlarm(product);
			                	if (alarmUpdatingQuery != null) {
			                		handle.execute(alarmUpdatingQuery);
			                	}
			        				}
			        			}
	            		}
		            
		            }
	            }
	        	}
          	handle.commit();
          }	
        } catch (Exception e) {
    			channel.basicAck(envelope.getDeliveryTag(), false);
          logger.error("Failed to handle status change", e);
        }
      }
		};

		logger.info(forWhichConsumer + " is up and running.");
		channel.basicConsume(queueDef.NAME, true, consumer);
  }
  
  private static String queryMakeAvailable(Long id, Link linkFromParser) {
    return
      String.format(
        "update link " + 
        "set sku='%s', name='%s', brand='%s', seller='%s', shipment='%s', price=%f, pre_status=status, status='%s', grup='%s', " +
        "platform_id=%d, retry=0, parse_code='OK', parse_problem=null, checked_at=now(), updated_at=now() " +
        "where id=%d",
        linkFromParser.getSku(),
        linkFromParser.getName(),
        linkFromParser.getBrand(),
        linkFromParser.getSeller(),
        linkFromParser.getShipment(),
        linkFromParser.getPrice(),
        linkFromParser.getStatus(),
        linkFromParser.getStatus().getGrup(),
        linkFromParser.getPlatformId(),
        id
      );
  }

  private static String queryIncreaseRetry(Link linkFromDb, Link linkFromParser) {
    return
      String.format(
        "update link " + 
        "set retry=%d, parse_code='%s', parse_problem=%s, checked_at=now(), updated_at=now() " +
        "where id=%d",
        linkFromDb.getRetry(),
        (linkFromParser.getParseCode() != null ? linkFromParser.getParseCode() : "OK"),
        (linkFromParser.getParseProblem() != null ? "'"+linkFromParser.getParseProblem()+"'" : "null"),
        linkFromDb.getId()
      );
  }

  private static String queryUpdatePrice(Long id, BigDecimal price, int priceDirection) {
    return
      String.format(
        "update link " + 
        "set price=%f, price_direction=%d, retry=0, parse_code='OK', parse_problem=null, checked_at=now(), updated_at=now() " +
        "where id=%d",
        price,
        priceDirection,
        id
      );
  }

  private static String queryClearActiveLink(Long id) {
    return
      String.format(
        "update link " + 
        "set retry=0, parse_code='OK', parse_problem=null, checked_at=now(), updated_at=now() " +
        "where id=%d",
        id
      );
  }

  private static String queryUpdateLinkStatus(Link link) {
  	return
			String.format(
				"update link " + 
					"set retry=%d, parse_code='%s', parse_problem=%s, pre_status=status, status='%s', grup='%s', checked_at=now(), updated_at=now(), " + 
					" platform_id= " + (link.getPlatformId() != null ? link.getPlatformId() : "null") +
					" where id=%d",
					link.getRetry(),
	        (link.getParseCode() != null ? link.getParseCode() : "OK"),
	        (link.getParseProblem() != null ? "'"+link.getParseProblem()+"'" : "null"),
					link.getStatus(),
					(link.getRetry() < 3 ? link.getStatus().getGrup() : Grup.PROBLEM),
					link.getId()
				);
  }

	private static String queryAddStatusHistory(Link linkFromDb, Link linkFromParser) {
  	return
      String.format(
          "insert into link_history (status, parse_problem, link_id, product_id, workspace_id) " +
          "values ('%s', %s, %d, %d, %d)",
          linkFromParser.getStatus(),
          (linkFromParser.getParseProblem() != null ? "'"+linkFromParser.getParseProblem()+"'" : "null"),
          linkFromDb.getId(),
          linkFromDb.getProductId(),
          linkFromDb.getWorkspaceId()
        );
  }

  private static List<String> queryRefreshSpecList(Link linkFromDb, Link linkFromParser) {
    List<String> list = new ArrayList<>();

    //deleting old specs
    list.add("delete from link_spec where link_id = " + linkFromDb.getId());

    // inserting new ones
    List<LinkSpec> specList = linkFromParser.getSpecList();
    if (CollectionUtils.isNotEmpty(specList)) {
      for (LinkSpec spec: specList) {
        list.add(
          String.format(
            "insert into link_spec (_key, _value, link_id, product_id, workspace_id) " +
            "values (%s, %s, %d, %d, %d)",
            (spec.getKey() != null ? "'"+spec.getKey()+"'" : "null"),
            (spec.getValue() != null ? "'"+spec.getValue()+"'" : "null"),
            linkFromDb.getId(),
            linkFromDb.getProductId(),
            linkFromDb.getWorkspaceId()
          )
        );
      }
    }

    return list;
  }

  private static String checkAndGenerateUpdateQueryForProductAlarm(Product product) {
  	boolean willBeUpdated = false;
  	
  	Alarm alarm = product.getAlarm();

  	if (AlarmSubject.POSITION.equals(alarm.getSubject())) {
  		switch (alarm.getSubjectWhen()) {
  			case EQUAL: {
  				willBeUpdated = product.getPosition().name().equals(alarm.getCertainPosition());
  				break;
  			}
  			case NOT_EQUAL: {
  				willBeUpdated = product.getPosition().name().equals(alarm.getCertainPosition()) == false;
  				break;
  			}
  			default: {
  				willBeUpdated = true;
  				break;
  			}
			}
  	}

  	BigDecimal newAmount = null;

  	if (AlarmSubject.POSITION.equals(alarm.getSubject()) == false) {
  		switch (alarm.getSubject()) {
  			case MINIMUM: {
  				newAmount = product.getMinPrice();
  				break;
  			}
  			case AVERAGE: {
  				newAmount = product.getAvgPrice();
  				break;
  			}
  			case MAXIMUM: {
  				newAmount = product.getMaxPrice();
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
          "update alarm set last_position='%s', last_amount=%f, %s updated_at=now() where id=%d ",
          product.getPosition(),
          newAmount,
          tobeNotifiedPart,
          product.getAlarmId()
        );
  	}

  	return null;
  }

  private static String checkAndGenerateUpdateQueryForLinkAlarm(Link linkFromDb, Link linkFromParser) {
  	boolean willBeUpdated = false;

  	Alarm alarm = linkFromDb.getAlarm();

  	if (linkFromDb.getStatus().equals(linkFromParser.getStatus()) == false && AlarmSubject.POSITION.equals(alarm.getSubject())) {
  		switch (alarm.getSubjectWhen()) {
  			case EQUAL: {
  				willBeUpdated = linkFromParser.getStatus().name().equals(alarm.getCertainPosition());
  				break;
  			}
  			case NOT_EQUAL: {
  				willBeUpdated = linkFromParser.getStatus().name().equals(alarm.getCertainPosition()) == false;
  				break;
  			}
  			default: {
  				willBeUpdated = true;
  				break;
  			}
			}
  	}
  	
  	if (linkFromDb.getPrice().compareTo(linkFromParser.getPrice()) != 0 && AlarmSubject.PRICE.equals(alarm.getSubject())) {
  		switch (alarm.getSubjectWhen()) {
  			case INCREASED: {
  				willBeUpdated = linkFromParser.getPrice().compareTo(alarm.getLastAmount()) > 0;
  				break;
  			}
  			case DECREASED: {
  				willBeUpdated = linkFromParser.getPrice().compareTo(alarm.getLastAmount()) < 0;
  				break;
  			}
  			case OUT_OF_LIMITS: {
  				willBeUpdated = checkIfPriceIsOutOfLimits(linkFromParser.getPrice(), alarm.getAmountLowerLimit(), alarm.getAmountUpperLimit());
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

    	//checks if it is already notified within 5 mins. if so, no need to disturb the user frequently!
    	if (willBeUpdated && alarm.getNotifiedAt() != null) {
        long diff = DateUtils.findMinuteDiff(alarm.getNotifiedAt(), new Date());
        if (diff <= 5) {
        	tobeNotifiedPart = "";
        }
    	}

      return
        String.format(
          "update alarm set last_position='%s', last_amount=%f, %s updated_at=now() where id=%d ",
          linkFromParser.getStatus(),
          linkFromParser.getPrice(),
          tobeNotifiedPart,
          linkFromDb.getAlarmId()
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
