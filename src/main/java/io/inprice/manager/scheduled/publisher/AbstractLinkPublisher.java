package io.inprice.manager.scheduled.publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import io.inprice.common.config.ScheduleDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
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

  //rabbitmq connection
  private Connection mqConn;

  abstract String getTaskName();
  abstract List<Link> findLinks(LinkDao linkDao);

  public AbstractLinkPublisher(ScheduleDef schedule, Connection conn) {
  	this.schedule = schedule;
		this.mqConn = conn;
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

            boolean shouldBeAddedToQueue = true;
            
            if (LinkStatus.TOBE_CLASSIFIED.equals(link.getStatus()) || LinkStatus.RESOLVED.equals(link.getStatus())) {
              LinkStatus oldStatus = link.getStatus();
              Platform platform = PlatformRepository.findByUrl(handle, link.getUrl());
              if (platform != null) {
              	link.setPlatform(platform);
                link.setPlatformId(platform.getId());
                if (platform.getStatus() != null) {
                  link.setStatus(platform.getStatus());
                }
              } else {
                link.setStatus(LinkStatus.TOBE_IMPLEMENTED);
              }

              //checks if the same url added previously. if so, then clones it!
              if (LinkStatus.TOBE_CLASSIFIED.equals(link.getStatus())) {
                Link sample = linkDao.findTheSameAndActiveLinkByUrl(link.getUrl());
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
                }
              }

              if (link.getStatus().equals(oldStatus) == false) {
              	shouldBeAddedToQueue = false;
              	//publish status change
              	try (Channel channel = mqConn.createChannel()) {
            	  	String outMessage = JsonConverter.toJson(new LinkStatusChange(link, oldStatus, link.getPrice()));
            	  	channel.basicPublish("", Props.getConfig().QUEUES.STATUS_CHANGING_LINKS.NAME, null, outMessage.getBytes());
              	} catch (IOException | TimeoutException e) {
                  logger.error("Failed to publish status changing link", e);
            		}
              }
            }
            if (shouldBeAddedToQueue) {
            	//publish link
            	try (Channel channel = mqConn.createChannel()) {
          	  	String outMessage = JsonConverter.toJson(link);
          	  	channel.basicPublish("", link.getPlatform().getQueue(), null, outMessage.getBytes());
            	} catch (IOException | TimeoutException e) {
                logger.error("Failed to publish link", e);
          		}
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

}
