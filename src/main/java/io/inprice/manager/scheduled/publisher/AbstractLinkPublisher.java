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

          	//if a link is published within 30 min then no need to send it again!
          	if (PublishedLinkChecker.hasAlreadyPublished(link.getUrlHash())) {
          		logger.info("{} already published in 30 mins!", link.getUrl());
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

}
