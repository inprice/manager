package io.inprice.manager.scheduled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
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

  private List<String> activeAccountStatuses;

  LinkPublisher(LinkStatus status) {
    this.status = status;
    if (LinkStatus.FAILED_GROUP.equals(status.getGroup())) {
      this.retryLimit = Props.RETRY_LIMIT_FOR(status);
    }
    this.activeAccountStatuses = 
      Arrays.asList(
        AccountStatus.FREE.name(),
        AccountStatus.COUPONED.name(),
        AccountStatus.SUBSCRIBED.name()
      );
    log.info("{} link publisher is up.", status);
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

      try (Handle handle = Database.getHandle()) {
        LinkDao linkDao = handle.attach(LinkDao.class);
  
        List<Link> links = findLinks(linkDao);
        while (links.size() > 0) { 
          counter += links.size();

          List<Long> linkIds = new ArrayList<>(links.size());
          for (Link link: links) {
            linkIds.add(link.getId());
            RedisClient.publishActiveLink(link);
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
        log.error("Failed to trigger " + this.status.name() , e);
      }

    } catch (Exception e) {
      log.error(String.format("Failed to completed %s task!", this.status), e);
    } finally {
      log.info("{} link(s) handled successfully. Count: {}, Time: {}", this.status, counter, (System.currentTimeMillis() - startTime));
      Global.stopTask(this.status.name());
    }

  }

  private List<Link> findLinks(LinkDao linkDao) {
    if (this.retryLimit < 1) {
      String extraCondition = (LinkStatus.TOBE_CLASSIFIED.equals(status) ? "l.last_check is null OR" : "");
      return 
        linkDao.findListByStatus(
          activeAccountStatuses,
          this.status.name(),
          Props.INTERVAL_FOR_LINK_COLLECTION(),
          Props.DB_FETCH_LIMIT(), extraCondition
        );
    } else {
      return
        linkDao.findFailedListByStatus(
          activeAccountStatuses,
          this.status.name(),
          Props.INTERVAL_FOR_LINK_COLLECTION(),
          this.retryLimit, Props.DB_FETCH_LIMIT()
        );
    }
  }

}
