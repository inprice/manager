package io.inprice.manager.scheduled;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.LinkDao;
import io.inprice.manager.helpers.Global;
import io.inprice.manager.helpers.RedisClient;

/**
 * Contains common functions used by all the publishers.
 * 
 * @author mdpinar
 */
class LinkPublisher implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(LinkPublisher.class);

  private LinkStatus status;
  private int retryLimit;

  LinkPublisher(LinkStatus status) {
    this.status = status;
    if (LinkStatus.FAILED_GROUP.equals(status.getGroup())) {
      this.retryLimit = Props.RETRY_LIMIT_FOR(status);
    }
    log.info("{} publisher is up.", status);
  }

  @Override
  public void run() {
    if (Global.isTaskRunning(this.status.name())) {
      log.warn("{} link handler is already triggered!", this.status);
      return;
    }

    int counter = 0;
    long startTime = System.currentTimeMillis();

    try {
      Global.startTask(this.status.name());

      List<Link> links = findLinks();
      while (links.size() > 0) {
        counter += links.size();

        handleLinks(links);
        setLastCheckTime(links);

        if (links.size() >= Props.DB_FETCH_LIMIT()) {
          try {
            Thread.sleep(Props.WAITING_TIME_FOR_FETCHING_LINKS());
          } catch (InterruptedException e) { }
          links = findLinks();
        } else {
          links.clear();
        }
      }

    } catch (Exception e) {
      log.error(String.format("Failed to completed %s task!", this.status), e);
    } finally {
      if (counter > 0) {
        log.info("{} link(s) handled successfully. Count: {}, Time: {}", 
          this.status, counter, (System.currentTimeMillis() - startTime));
      } else {
        log.info("No {} link found!", status);
      }
      Global.stopTask(this.status.name());
    }

  }

  private List<Link> findLinks() {
    try (Handle handle = Database.getHandle()) {
      LinkDao linkDao = handle.attach(LinkDao.class);
      if (this.retryLimit < 1) {
        return linkDao.findListByStatus(this.status.name(), Props.DB_FETCH_LIMIT());
      } else {
        return linkDao.findFailedListByStatus(this.status.name(), this.retryLimit, Props.DB_FETCH_LIMIT());
      }
    }
  }

  private void handleLinks(List<Link> links) {
    for (Link link: links) {
      RedisClient.publish(link);
    }
  }

  private void setLastCheckTime(List<Link> links) {
    //gathering all ids
    List<Long> idList = new ArrayList<>(links.size());
    for (Link link: links) {
      idList.add(link.getId());
    }

    //updating their last check date
    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transaction -> {
        LinkDao linkDao = transaction.attach(LinkDao.class);
        linkDao.setLastCheckTime(idList, (this.retryLimit > 0 ? 1 : 0));
        return true;
      });
    }
  }

}
