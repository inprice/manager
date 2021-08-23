package io.inprice.manager.scheduled.publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;

import io.inprice.common.config.ScheduleDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.info.LinkStatusChange;
import io.inprice.common.info.ParseStatus;
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

  private Channel scrappingLinksChannel;
  private Channel statusChangingLinksChannel;

  abstract String getTaskName();
  abstract List<Link> findLinks(LinkDao linkDao);

  public AbstractLinkPublisher(ScheduleDef schedule, Channel scrappingLinksChannel, Channel statusChangingLinksChannel) {
  	this.schedule = schedule;
  	this.scrappingLinksChannel = scrappingLinksChannel;
  	this.statusChangingLinksChannel = statusChangingLinksChannel;
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
        if (links.size() > 0) Collections.shuffle(links);

        while (links.size() > 0) {
          counter += links.size();

          List<Long> linkIds = new ArrayList<>(links.size());
          for (Link link: links) {

          	linkIds.add(link.getId());
          	if (link.getPlatform() != null && link.getPlatform().getParked().equals(Boolean.TRUE)) continue;

            LinkStatus oldStatus = link.getStatus();
            Long oldPlatformId = link.getPlatformId();

            //no platform has been specified yet then try to look up the same url to clone it
            if (link.getPlatformId() == null) {
              Link sample = linkDao.findTheSameLinkByUrl(link.getUrl());
              if (sample != null) {
              	link.setSku(sample.getSku());
              	link.setName(sample.getName());
              	link.setBrand(sample.getBrand());
              	link.setSeller(sample.getSeller());
              	link.setShipment(sample.getShipment());
              	link.setPrice(sample.getPrice());
              	link.setLevel(sample.getLevel());
                link.setPreStatus(link.getStatus());
                link.setStatus(sample.getStatus());
                link.setPlatformId(sample.getPlatformId());
                link.setPlatform(sample.getPlatform());
              }
            }

            //not found, try to find the platform by url
            if (link.getPlatformId() == null) {
              Platform platform = PlatformRepository.findByUrl(handle, link.getUrl());
              if (platform != null) {
              	link.setPlatformId(platform.getId());
              	link.setPlatform(platform);
              }
            }

            if (link.getPlatformId() != null && Boolean.TRUE.equals(link.getPlatform().getParked())) {
          		if (link.getPlatformId().equals(oldPlatformId) == false) {
          			setNewPlatform(link);
          		}
          		continue;
          	}
            
            ParseStatus newParseStatus = null;

            if (link.getPlatformId() == null) {
            	newParseStatus = new ParseStatus(ParseStatus.CODE_NOT_IMPLEMENTED, "Not implemented yet");
            } else {
            	if (Boolean.TRUE.equals(link.getPlatform().getBlocked())) {
            		newParseStatus = new ParseStatus(ParseStatus.HTTP_CODE_NOT_ALLOWED, "Not allowed");
            	}
            }

            if (link.getStatus().equals(oldStatus) || newParseStatus == null) {
            	publishForScrapping(link);
            } else {
            	publishForStatusChanging(link, newParseStatus);
            }
          }
          linkDao.bulkUpdateCheckedAt(linkIds);

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
	  	String outMessage = JsonConverter.toJson(link);
	  	scrappingLinksChannel.basicPublish("", link.getPlatform().getQueue(), null, outMessage.getBytes());
  	} catch (IOException e) {
      logger.error("Failed to publish link", e);
		}
  }

  private void publishForStatusChanging(Link link, ParseStatus newParseStatus) {
  	try {
	  	String outMessage = JsonConverter.toJson(new LinkStatusChange(link, newParseStatus, link.getPrice()));
	  	statusChangingLinksChannel.basicPublish("", Props.getConfig().QUEUES.STATUS_CHANGING_LINKS.NAME, null, outMessage.getBytes());
  	} catch (IOException e) {
      logger.error("Failed to publish status changing link", e);
		}
  }

  private void setNewPlatform(Link link) {
    try (Handle handle = Database.getHandle()) {
    	LinkDao dao = handle.attach(LinkDao.class);
    	dao.setPlatform(link.getId(), link.getPlatformId(), link.getStatus());
    } catch (Exception e) {
      logger.error("Failed to set platform id", e);
    }
  }

}
