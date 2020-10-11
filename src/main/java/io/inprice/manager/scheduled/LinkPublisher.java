package io.inprice.manager.scheduled;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.info.TimePeriod;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.models.Site;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.LinkDao;
import io.inprice.manager.helpers.Global;
import io.inprice.manager.helpers.RedisClient;
import io.inprice.manager.helpers.SiteFinder;

/**
 * Contains common functions used by all the publishers.
 * Since quartz needs to create publisher classes through their default (without args)
 * constructors, in each publisher, please add a default constructor referencing
 * this class's constructors.
 *
 * @author mdpinar
 */
class LinkPublisher implements Task {

  private static final Logger log = LoggerFactory.getLogger("StandardLinkPublisher");

  private LinkStatus status;
  private TimePeriod timePeriod;
  private int retryLimit;

  LinkPublisher(LinkStatus status, String timePeriodStatement) {
    this(status, timePeriodStatement, 0);
  }

  LinkPublisher(LinkStatus status, String timePeriodStatement, int retryLimit) {
    this.status = status;
    this.timePeriod = DateUtils.parseTimePeriod(timePeriodStatement);
    this.retryLimit = retryLimit;
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
      }
      Global.stopTask(this.status.name());
    }

  }

  @Override
  public TimePeriod getTimePeriod() {
    return this.timePeriod;
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

      switch (link.getStatus()) {
        case TOBE_CLASSIFIED: {
          Site site = SiteFinder.findSiteByUrl(link.getUrl());
          if (site != null) {
            link.setSiteId(site.getId());
            link.setWebsiteClassName(site.getClassName());
            if (site.getStatus() != null) {
              LinkStatus siteStatus = LinkStatus.valueOf(site.getStatus());
              if (!LinkStatus.AVAILABLE.equals(siteStatus)) link.setStatus(siteStatus);
            } else {
              link.setStatus(LinkStatus.CLASSIFIED);
            }
          } else {
            link.setStatus(LinkStatus.TOBE_IMPLEMENTED);
            //TODO: bu durumda publish etmeye gerek yok!
            //common projesindeki CommonRepository ile sadece db level statu degisikligi yeterli olacaktır!
          }
          break;
        }
        default:
          break;
      }

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
