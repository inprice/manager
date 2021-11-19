package io.inprice.manager.consumer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.jdbi.v3.core.Handle;
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
import io.inprice.common.formula.EvaluationResult;
import io.inprice.common.formula.FormulaHelper;
import io.inprice.common.helpers.AlarmHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.ProductRefreshResult;
import io.inprice.common.meta.Grup;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.models.Product;
import io.inprice.common.models.SmartPrice;
import io.inprice.common.repository.CommonDao;
import io.inprice.manager.dao.AlarmDao;

/**
 * This is the most important class to mange links' statuses, prices and alarms.
 *
 * The main reason for the complexity in sql clauses here is that
 * each update/insert/delete for a link is also applied on all the other links having the same url so that no need to handle the same urls again and again
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

          try (Handle zhandle = Database.getHandle()) {
          	zhandle.useTransaction(trans -> { 
            	CommonDao commonDao = trans.attach(CommonDao.class);
            	AlarmDao alarmDao = trans.attach(AlarmDao.class);

  	        	List<Link> linksFromDb = commonDao.findAllLinksByHash(linkFromParser.getUrlHash());

  	        	if (CollectionUtils.isNotEmpty(linksFromDb)) {
  	        		for (Link linkFromDb: linksFromDb) {

  			          boolean hasStatusChanged = (linkFromParser.getStatus().equals(linkFromDb.getStatus()) == false);

  			          boolean willPriceBeRefreshed = false;
  			          if (hasStatusChanged) {
  			          	willPriceBeRefreshed = (linkFromDb.getStatus().equals(LinkStatus.AVAILABLE) || linkFromParser.getStatus().equals(LinkStatus.AVAILABLE));
  			          } else {
  			          	willPriceBeRefreshed = (linkFromParser.getStatus().equals(LinkStatus.AVAILABLE) && linkFromParser.getPrice().compareTo(linkFromDb.getPrice()) != 0);
  			          }

  			          switch (linkFromParser.getStatus().getGrup()) {
  			          	case ACTIVE: {
  				          	if (hasStatusChanged) {
  				          		trans.execute(queryMakeAvailable(linkFromDb.getId(), linkFromParser));
  				          		List<String> queries = queryRefreshSpecList(linkFromDb, linkFromParser);
  				          		for (String query: queries) {
  				          			trans.execute(query);
  				          		}
  				          	} else if (willPriceBeRefreshed) {
  				          		trans.execute(queryUpdatePrice(linkFromDb.getId(), linkFromParser.getPrice(), linkFromParser.getPrice().compareTo(linkFromDb.getPrice())));
  				          	} else if (linkFromDb.getRetry() > 0 || linkFromDb.getParseCode().equals(linkFromParser.getParseCode()) == false) {
  				          		trans.execute(queryClearActiveLink(linkFromDb.getId()));
  				          	}
  										break;
  									}
  			
  			          	case TRYING: {
  			          		willPriceBeRefreshed = false;
  		            		if (linkFromDb.getRetry() < 2) { //retry starts from zero not one!
  			            		hasStatusChanged = false;
  			            		trans.execute(queryIncreaseRetry(linkFromDb, linkFromParser));
  			        			} else {
  			        				hasStatusChanged = true;
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
  			        			trans.execute(queryUpdateLinkStatus(linkFromDb.getGrup(), linkFromParser));
  			        		}
  			        		trans.execute(queryAddStatusHistory(linkFromDb, linkFromParser));
  			          }

  		            //check if link is alarmed and conditions are suitable for firing an alarm.
  		            if ((hasStatusChanged || willPriceBeRefreshed) && linkFromDb.getAlarmId() != null) {
  		            	Alarm alarm = alarmDao.findById(linkFromDb.getAlarmId());
  		            	String alarmUpdatingQuery = AlarmHelper.generateAlarmUpdateQueryForLink(linkFromDb, linkFromParser, alarm);
  		            	if (alarmUpdatingQuery != null) {
  		            		trans.execute(alarmUpdatingQuery);
  	            		}
  		        		}

  		        		//if price is changed then we need to calculate diff to add a new row in to price history
  		        		//and also, there may an alarm on the product of those links sensitive for changings like min/max/avg
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
  	              	} else if (hasStatusChanged) {
  	              		commonDao.insertLinkPrice(linkFromDb.getId(), linkFromParser.getPrice(), linkFromDb.getProductId(), linkFromDb.getWorkspaceId());
  	              	}

  	              	Product oldProduct = commonDao.findProductById(linkFromDb.getProductId());
  	              	ProductRefreshResult prr = ProductRefreshResultConverter.convert(commonDao.refreshProduct(linkFromDb.getProductId()));

  	              	//alarming
  	              	if (oldProduct.getAlarmId() != null) {
  	              		Alarm alarm = alarmDao.findById(oldProduct.getAlarmId());
  	                	String alarmUpdatingQuery = AlarmHelper.generateAlarmUpdateQueryForProduct(oldProduct, prr, alarm);
  	                	if (alarmUpdatingQuery != null) {
  	                		trans.execute(alarmUpdatingQuery);
  	                	}
  	              	}

  	              	//smart pricing
  	              	if (prr.getSmartPriceId() != null) {
  	              		SmartPrice smartPrice = commonDao.findById(oldProduct.getSmartPriceId());
  	                	String smartPriceUpdatingQuery = generateUpdateQueryForSuggestedPrice(oldProduct.getId(), smartPrice, prr);
  	                	if (smartPriceUpdatingQuery != null) {
  	                		trans.execute(smartPriceUpdatingQuery);
  	                	}
  	              	}
  		            }
  	        		}
  	        	}
          	});
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
        linkFromDb.getRetry()+1,
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

  private static String queryUpdateLinkStatus(Grup oldGrup, Link link) {
  	Grup newGrup = link.getStatus().getGrup();
  	if (link.getRetry() >= 2) {
  		if (oldGrup.equals(Grup.ACTIVE) || oldGrup.equals(Grup.WAITING)) {
  			newGrup = Grup.TRYING;
  		} else {
  			newGrup = Grup.PROBLEM;
  		}
  	}
  	return
			String.format(
				"update link " + 
					"set retry=0, parse_code='%s', parse_problem=%s, pre_status=status, status='%s', grup='%s', checked_at=now(), updated_at=now(), " + 
					" platform_id= " + (link.getPlatformId() != null ? link.getPlatformId() : "null") +
					" where id=%d",
	        (link.getParseCode() != null ? link.getParseCode() : "OK"),
	        (link.getParseProblem() != null ? "'"+link.getParseProblem()+"'" : "null"),
					link.getStatus(),
					newGrup,
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

  private static String generateUpdateQueryForSuggestedPrice(long productId, SmartPrice smartPrice, ProductRefreshResult prr) {
  	EvaluationResult result = FormulaHelper.evaluate(smartPrice, prr);
    return
      String.format(
        "update product set suggested_price=%f, suggested_price_problem=%s where id=%d ",
        result.getValue(),
        (result.getProblem() != null ? "'"+result.getProblem()+"'" : "null"),
        productId
      );
  }

}
