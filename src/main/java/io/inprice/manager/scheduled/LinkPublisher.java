package io.inprice.manager.scheduled;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.models.Platform;
import io.inprice.common.repository.PlatformRepository;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.LinkDao;
import io.inprice.manager.helpers.Global;
import io.inprice.manager.helpers.RedisClient;

/**
 * Publishes active links.
 * 
 * @author mdpinar
 */
class LinkPublisher implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(LinkPublisher.class);
  
  private String taskName;
  private StringBuilder condition;
  private List<LinkStatus> linkStatuses;

  LinkPublisher(boolean isForActives, int retry) {
  	if (isForActives) {
  		this.taskName = "ACTIVE-LINKS-PUBLISHER-" + retry;
  		this.linkStatuses = LinkStatus.ACTIVE_STATUSES;
  	} else {
  		this.taskName = "FAILED-LINKS-PUBLISHER-" + retry;
  		this.linkStatuses = LinkStatus.FAILED_STATUSES;
  	}

  	condition = new StringBuilder("retry = ");
  	condition.append(retry);
  	condition.append(" AND ");
  	
  	String majorTiming = (SysProps.APP_ENV().equals(AppEnv.PROD) ? " hour" : " minute");
  	
    if (retry == 0) {
    	condition.append("(l.last_check is null OR l.last_check <= now() - interval 30 minute)");
    } else {
    	condition.append("l.last_check <= now() - interval " + retry + majorTiming);
    }

    log.info("{} is up with a retry value: {}.", taskName, retry);
  }

  private List<Link> findLinks(LinkDao linkDao) {
    return 
      linkDao.findListByStatus(
        AccountStatus.ACTIVE_STATUSES,
        linkStatuses,
        condition.toString()
      );
  }

  @Override
  public void run() {
    if (Global.isTaskRunning(taskName)) {
      log.warn("{} is already triggered!", taskName);
      return;
    }

    int counter = 0;
    long startTime = System.currentTimeMillis();

    try {
      Global.startTask(taskName);

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
                if (platform.getStatus() != null) {
                  link.setStatus(platform.getStatus());
                  link.setProblem(platform.getProblem());
                }
              } else {
                link.setStatus(LinkStatus.TOBE_IMPLEMENTED);
              }
              if (!link.getStatus().equals(oldStatus)) {
              	shouldBeAddedToQueue = false;
                RedisClient.publishStatusChange(link, oldStatus);
              }
            }
            if (shouldBeAddedToQueue) {
            	RedisClient.publishActiveLink(link);
            }
          }
          linkDao.bulkUpdateLastCheck(linkIds);

          if (links.size() >= Props.DB_FETCH_LIMIT()) {
            try {
              Thread.sleep(Props.WAITING_TIME_FOR_FETCHING_LINKS());
            } catch (InterruptedException e) { }
            links = findLinks(linkDao);
          } else {
            links.clear();
          }
        }
      } catch (Exception e) {
        log.error(taskName + " failed to trigger!" , e);
      }

    } catch (Exception e) {
      log.error(String.format("%s failed to complete!", taskName), e);
    } finally {
      log.info("{} completed successfully. Count: {}, Time: {}", taskName, counter, (System.currentTimeMillis() - startTime));
      Global.stopTask(taskName);
    }

  }

}
