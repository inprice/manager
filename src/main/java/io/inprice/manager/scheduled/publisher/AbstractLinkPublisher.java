package io.inprice.manager.scheduled.publisher;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;

import io.inprice.common.config.SchedulerDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.meta.Grup;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.models.Platform;
import io.inprice.common.repository.CommonDao;
import io.inprice.common.repository.PlatformRepository;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.LinkDao;
import io.inprice.manager.scheduled.Task;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Publishes links to RabbitMQ
 * 
 * @author mdpinar
 */
abstract class AbstractLinkPublisher implements Task {

  private static final Logger logger = LoggerFactory.getLogger(AbstractLinkPublisher.class);

  private SchedulerDef schedule;

  private Channel scrappingLinksChannel;
  private Channel statusChangingLinksChannel;

  abstract String getTaskName();
  abstract List<Link> findLinks(LinkDao linkDao);

  public AbstractLinkPublisher(SchedulerDef schedule, Channel scrappingLinksChannel, Channel statusChangingLinksChannel) {
  	this.schedule = schedule;
  	this.scrappingLinksChannel = scrappingLinksChannel;
  	this.statusChangingLinksChannel = statusChangingLinksChannel;
	}

  @Override
  public SchedulerDef getScheduler() {
  	return schedule;
  }

  @Override
  public void run() {
    if (TaskManager.isTaskRunning(getTaskName())) {
      logger.warn("{} is already triggered!", getTaskName());
      return;
    }
    
    int counter = 0;
    long startTime = System.currentTimeMillis();

    try {
      TaskManager.startTask(getTaskName());

      try (Handle handle = Database.getHandle()) {
        LinkDao linkDao = handle.attach(LinkDao.class);
  
        List<Link> links = findLinks(linkDao);
        if (links.size() > 0) Collections.shuffle(links);

        while (links.size() > 0) {
          counter += links.size();

          Set<String> linkHashes = new HashSet<>(links.size());
          for (Link link: links) {

          	//if a link is published within reviewPeriod (specified in config.json) min then no need to send it again!
          	if (PublishedLinkChecker.hasAlreadyPublished(link.getUrlHash())) {
          		logger.info("{} already published in {} mins!", link.getUrl(), Props.getConfig().APP.LINK_REVIEW_PERIOD);
          		continue;
          	}
          	PublishedLinkChecker.published(link.getUrlHash()); //for the check just above

          	linkHashes.add(link.getUrlHash());

          	/*
          	 * Please note:
          	 * this class is responsible for publishing all active and tobe classified links.
          	 * some of the links may have special statuses now, like being blocked or parked.
          	 * so the controls placed below are for checking special situations.
          	 */
            LinkStatus oldStatus = link.getStatus();

            //no platform has been specified yet then try to search for the same url in db to clone it
            boolean isCloned = false;
            if (link.getPlatformId() == null) {
              Link source = linkDao.findSameLinkByUrlHash(link.getUrlHash());
              if (source != null) {
                //we do not push the link to StatusChangingLinkConsumer because it will try to unnecessarily update similar links once more
                //we have to handle all the clone operation here!
              	isCloned = true;
              	cloneLink(source, link);
              }
            }

            if (isCloned == false) {
	            //not found! try to find the platform by url
	            if (link.getPlatformId() == null) {
	              Platform platform = PlatformRepository.findByUrl(handle, link.getUrl());
	              if (platform != null) {
	              	link.setPlatformId(platform.getId());
	              	link.setPlatform(platform);
	              } else {
	              	//still not found then it must be implemented
	              	link.setStatus(LinkStatus.TOBE_IMPLEMENTED);
	              }
	            }
	
	            //check if it is parked or blocked
	            if (link.getPlatform() != null) {
	            	if (link.getPlatform().getParked()) continue;
	            	if (link.getPlatform().getBlocked()) link.setStatus(LinkStatus.NOT_ALLOWED);
	          	}
	
	            if (link.getStatus().equals(oldStatus)) {
	            	publishForScrapping(link);
	            } else {
	            	publishForStatusChanging(link);
	            }
            }
          }
          if (linkHashes.size() > 0) linkDao.bulkUpdateCheckedAt(linkHashes);

          if (links.size() >= Props.getConfig().LIMITS.LINK_LIMIT_FETCHING_FROM_DB) {
            try {
              Thread.sleep(Props.getConfig().LIMITS.WAIT_LIMIT_BEFORE_NEXT_FETCH);
            } catch (InterruptedException e) { }
            links = findLinks(linkDao);
            if (links.size() > 0) Collections.shuffle(links);
          } else {
            links.clear();
          }
        }
      } catch (Exception e) {
        logger.error(getTaskName() + " failed to trigger!" , e);
      }

    } catch (Exception e) {
      logger.error(String.format("%s failed to complete!", getTaskName()), e);
    } finally {
    	if (counter > 0) {
    		logger.info("{} completed successfully. Count: {}, Time: {}", getTaskName(), counter, (System.currentTimeMillis() - startTime));
    	}
      TaskManager.stopTask(getTaskName());
    }
  }

  private void publishForScrapping(Link link) {
  	try {
	  	String message = JsonConverter.toJsonWithoutIgnoring(link);
	  	scrappingLinksChannel.basicPublish("", link.getPlatform().getQueue(), null, message.getBytes());
  	} catch (IOException e) {
      logger.error("Failed to publish link", e);
		}
  }

  private void publishForStatusChanging(Link link) {
  	try {
	  	String message = JsonConverter.toJsonWithoutIgnoring(link);
	  	statusChangingLinksChannel.basicPublish("", Props.getConfig().QUEUES.STATUS_CHANGING_LINKS.NAME, null, message.getBytes());
  	} catch (IOException e) {
      logger.error("Failed to publish status changing link", e);
		}
  }

  /**
   * Cloning must be handled here since StatusChangingConsumer will unnecessarily try to update all the same links
   * 
   * @param source link
   * @param target link
   */
  private void cloneLink(Link source, Link target) {
    try (Handle handle = Database.getHandle()) {
    	handle.begin();

    	//copy each value from source to target link
			handle.execute(
				"update link " + 
				"set sku=?, name=?, brand=?, seller=?, shipment=?, price=?, price_direction=?, platform_id=?, " +
				"parse_code=?, parse_problem=?, pre_status=status, status=?, grup=?, checked_at=now(), updated_at=now() " + 
				"where id=?",
				source.getSku(), source.getName(), source.getBrand(), source.getSeller(), source.getShipment(),
				source.getPrice(), source.getPriceDirection(), source.getPlatformId(), source.getParseCode(), 
				source.getParseProblem(), source.getStatus(), source.getGrup(), 
				target.getId()
			);

    	//derive new history row for target
			handle.execute(
				"insert into link_history (link_id, status, parse_problem, product_id, workspace_id) " + 
				"select ?, status, parse_problem, ?, ? from link_history " +
				"where link_id=? " +
				"order by created_at desc " +
		    "limit 1",
				target.getId(), target.getProductId(), target.getWorkspaceId(),
				source.getId()
			);

    	//derive new price row
			handle.execute(
				"insert into link_price (link_id, old_price, new_price, diff_amount, diff_rate, product_id, workspace_id) " + 
				"select ?, old_price, new_price, diff_amount, diff_rate, ?, ? from link_price " +
				"where link_id=? " +
				"order by created_at desc " +
		    "limit 1",
				target.getId(), target.getProductId(), target.getWorkspaceId(),
				source.getId()
			);

    	//copy each spec row for target
			handle.execute(
				"insert into link_spec (link_id, _key, _value, product_id, workspace_id) " + 
				"select ?, _key, _value, ?, ? from link_spec " +
				"where link_id=? " +
				"order by _key ",
				target.getId(), target.getProductId(), target.getWorkspaceId(),
				source.getId()
			);

			//to refresh sums and indicators
			if (Grup.ACTIVE.equals(source.getGrup())) {
				//updates sums
				handle.attach(CommonDao.class).refreshProduct(target.getProductId());
			} else {
				//adjusts the indicators
				String newGrup = source.getGrup().name().toLowerCase() + "s";
				handle.execute(
					"update product " + 
					"set waitings=waitings-1, " + 
						newGrup + "=" + newGrup + "+1 " +
					"where id= " + target.getProductId()
				);
			}

			handle.commit();
  	} catch (Exception e) {
  		logger.error("Failed to clone Source Id: "+source.getId()+", Target Id: " + target.getId(), e);
  	}
  }

}
