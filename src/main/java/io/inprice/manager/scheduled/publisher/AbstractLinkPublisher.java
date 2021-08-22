package io.inprice.manager.scheduled.publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;

import io.inprice.common.config.ScheduleDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.info.LinkPlatformChange;
import io.inprice.common.info.LinkStatusChange;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.models.Platform;
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

  private ScheduleDef schedule;

  private Channel channelForScrapping;
  private Channel channelForStatusChanging;
  private Channel channelForPlatformChanging;

  abstract String getTaskName();
  abstract List<Link> findLinks(LinkDao linkDao);

  public AbstractLinkPublisher(ScheduleDef schedule, Channel chForScrapping, 
  		Channel chForStatusChanging, Channel chForPlatformChanging) {
  	this.schedule = schedule;
  	this.channelForScrapping = chForScrapping;
  	this.channelForStatusChanging = chForStatusChanging;
  	this.channelForPlatformChanging = chForPlatformChanging;
	}

  @Override
  public ScheduleDef getSchedule() {
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
      logger.info(getTaskName() + " is triggered.");

      try (Handle handle = Database.getHandle()) {
        LinkDao linkDao = handle.attach(LinkDao.class);
  
        List<Link> links = findLinks(linkDao);
        while (links.size() > 0) {
          counter += links.size();

          List<Long> linkIds = new ArrayList<>(links.size());
          for (Link link: links) {

          	linkIds.add(link.getId());
          	if (link.getPlatform() != null && link.getPlatform().getParked().equals(Boolean.TRUE)) continue;

            LinkStatus oldStatus = link.getStatus();
            Long oldPlatformId = link.getPlatformId();

            //no platform has been specified yet then try to find the same url to clone
            if (link.getPlatformId() == null) {
              Link sample = linkDao.findTheSameLinkByUrl(link.getUrl()); //returns with non-null platform!
              if (sample != null) {
              	link.setSku(sample.getSku());
              	link.setName(sample.getName());
              	link.setBrand(sample.getBrand());
              	link.setSeller(sample.getSeller());
              	link.setShipment(sample.getShipment());
              	link.setPrice(sample.getPrice());
              	link.setLevel(sample.getLevel());
                link.setPreStatus(sample.getPreStatus());
                link.setStatus(sample.getStatus());
                link.setPlatformId(sample.getPlatformId());
                link.setPlatform(sample.getPlatform());
                if (sample.getPlatform().getStatus() != null) link.setStatus(sample.getPlatform().getStatus());
              }
            }

            //still not found, try to find the platform by url
            if (link.getPlatformId() == null) {
              Platform platform = PlatformRepository.findByUrl(handle, link.getUrl());
              if (platform != null) {
              	link.setPlatformId(platform.getId());
              	link.setPlatform(platform);
                if (platform.getStatus() != null) link.setStatus(platform.getStatus());
              }
            }

            //still not found, it must be implemented
            if (link.getPlatformId() == null) {
            	link.setStatus(LinkStatus.TOBE_IMPLEMENTED);
            }

            //it has a platform but parked!
            if (link.getPlatform() != null && Boolean.TRUE.equals(link.getPlatform().getParked())) {
          		if (link.getPlatformId().equals(oldPlatformId) == false) publishForPlatformChanging(link);
          		continue;
            }

            if (link.getStatus().equals(oldStatus)) {
            	publishForScrapping(link);
            } else {
            	publishForStatusChanging(link, oldStatus);
            }
          }
          linkDao.bulkUpdateCheckedAt(linkIds);

          if (links.size() >= Props.getConfig().LIMITS.LINK_LIMIT_FETCHING_FROM_DB) {
            try {
              Thread.sleep(Props.getConfig().LIMITS.WAIT_LIMIT_BEFORE_NEXT_FETCH);
            } catch (InterruptedException e) { }
            links = findLinks(linkDao);
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
	  	String outMessage = JsonConverter.toJson(link);
	  	channelForScrapping.basicPublish("", link.getPlatform().getQueue(), null, outMessage.getBytes());
  	} catch (IOException e) {
      logger.error("Failed to publish link", e);
		}
  }

  private void publishForStatusChanging(Link link, LinkStatus oldStatus) {
  	try {
	  	String outMessage = JsonConverter.toJson(new LinkStatusChange(link, oldStatus, link.getPrice()));
	  	channelForStatusChanging.basicPublish("", Props.getConfig().QUEUES.STATUS_CHANGING_LINKS.NAME, null, outMessage.getBytes());
  	} catch (IOException e) {
      logger.error("Failed to publish status changing link", e);
		}
  }

  private void publishForPlatformChanging(Link link) {
  	try {
	  	String outMessage = JsonConverter.toJson(new LinkPlatformChange(link.getId(), link.getPlatformId(), link.getStatus()));
	  	channelForPlatformChanging.basicPublish("", Props.getConfig().QUEUES.PLATFORM_CHANGING_LINKS.NAME, null, outMessage.getBytes());
  	} catch (IOException e) {
      logger.error("Failed to publish platform changing link", e);
		}
  }

}
