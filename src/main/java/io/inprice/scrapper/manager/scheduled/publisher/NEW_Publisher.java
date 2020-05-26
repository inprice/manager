package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.common.utils.URLUtils;
import io.inprice.scrapper.manager.external.Props;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.SiteFinder;

/**
 * Finds and handles NEW (and also RENEWED through inheritance) links
 *
 * @author mdpinar
 */
public class NEW_Publisher extends AbstractLinkPublisher {

  @Override
  LinkStatus getStatus() {
    return LinkStatus.NEW;
  }

  @Override
  String getMQRoutingKey() {
    return Props.MQ_ROUTING_NEW_LINKS() + "." + getStatus().name();
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_NEW_LINKS();
  }

  @Override
  void handleLinks(List<Link> linkList) {
    for (Link link : linkList) {
      LinkStatus oldStatus = link.getStatus();

      if (URLUtils.isAValidURL(link.getUrl())) {
        Site site = SiteFinder.findSiteByUrl(link.getUrl());
        if (site != null) {
          link.setSiteId(site.getId());
          link.setWebsiteClassName(site.getClassName());
        } else {
          link.setStatus(LinkStatus.BE_IMPLEMENTED);
        }
      } else {
        link.setStatus(LinkStatus.IMPROPER);
      }

      if (link.getStatus().equals(oldStatus)) {
        // the consumer class is in Worker, NewLinksConsumer
        RabbitMQ.publish(getMQRoutingKey(), link);
      } else {
        // the consumer class is here, StatusChangeConsumer
        StatusChange change = new StatusChange(link, oldStatus);
        RabbitMQ.publish(Props.MQ_EXCHANGE_CHANGES(), Props.MQ_ROUTING_STATUS_CHANGES(), change); 
      }
    }
  }

}
