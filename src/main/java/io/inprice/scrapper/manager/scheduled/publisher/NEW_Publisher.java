package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.JsonConverter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.common.utils.URLUtils;
import io.inprice.scrapper.manager.config.Props;
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
    return SysProps.MQ_NEW_LINKS_ROUTING() + "." + getStatus().name();
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_NEW_LINKS();
  }

  @Override
  void handleLinks(List<Link> linkList) {
    Channel channel = RabbitMQ.openChannel();
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
        RabbitMQ.publishLink(channel, getMQRoutingKey(), JsonConverter.toJson(link));
      } else {
        // the consumer class is here, StatusChangeConsumer
        StatusChange change = new StatusChange(link, oldStatus);
        RabbitMQ.publish(channel, SysProps.MQ_CHANGES_EXCHANGE(), SysProps.MQ_STATUS_CHANGES_ROUTING(), JsonConverter.toJson(change));
      }
    }
    RabbitMQ.closeChannel(channel);
  }

}
