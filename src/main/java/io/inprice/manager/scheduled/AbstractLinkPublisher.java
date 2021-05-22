package io.inprice.manager.scheduled;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
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
abstract class AbstractLinkPublisher implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(AbstractLinkPublisher.class);
  
  abstract String getTaskName();
  abstract List<Link> findLinks(LinkDao linkDao);

  @Override
  public void run() {
  	final String taskName = getTaskName();

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
                link.setPlatformId(platform.getId());
                if (platform.getStatus() != null) {
                  link.setStatus(platform.getStatus());
                  link.setProblem(platform.getProblem());
                }
              } else {
                link.setStatus(LinkStatus.TOBE_IMPLEMENTED);
              }

              //TODO: burada daha evvelden bu link bir sekilde sisteme eklenmis mi diye bakilacak,
            	//varsa klonlanacak, yoksa asagidaki kisim isleyecek!
              if (!link.getStatus().equals(oldStatus)) {
              	shouldBeAddedToQueue = false;
                RedisClient.publishStatusChange(link, oldStatus);
              }
            }
            if (shouldBeAddedToQueue) {
            	RedisClient.publishActiveLink(link);
            }
          }
          linkDao.bulkUpdateCheckedAt(linkIds);

          if (links.size() >= Props.DB_FETCH_LIMIT) {
            try {
              Thread.sleep(Props.WAITING_TIME_FOR_FETCHING_LINKS);
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
