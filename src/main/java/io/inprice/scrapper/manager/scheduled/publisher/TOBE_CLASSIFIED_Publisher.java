package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.JsonConverter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.common.models.Competitor;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.common.utils.URLUtils;
import io.inprice.scrapper.manager.config.Props;
import io.inprice.scrapper.manager.helpers.SiteFinder;

/**
 * Finds and handles NEW (and also RENEWED through inheritance) competitors
 *
 * @author mdpinar
 */
public class TOBE_CLASSIFIED_Publisher extends AbstractCompetitorPublisher {

  @Override
  CompetitorStatus getStatus() {
    return CompetitorStatus.TOBE_CLASSIFIED;
  }

  @Override
  String getMQRoutingKey() {
    return SysProps.MQ_TOBE_CLASSIFIED_COMPETITORS_ROUTING() + "." + getStatus().name();
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_TOBE_CLASSIFIED_COMPETITORS();
  }

  @Override
  void handleCompetitors(List<Competitor> competitorList) {
    Channel channel = RabbitMQ.openChannel();
    for (Competitor competitor : competitorList) {
      CompetitorStatus oldStatus = competitor.getStatus();

      if (URLUtils.isAValidURL(competitor.getUrl())) {
        Site site = SiteFinder.findSiteByUrl(competitor.getUrl());
        if (site != null) {
          competitor.setSiteId(site.getId());
          competitor.setWebsiteClassName(site.getClassName());
        } else {
          competitor.setStatus(CompetitorStatus.TOBE_IMPLEMENTED);
        }
      } else {
        competitor.setStatus(CompetitorStatus.IMPROPER);
      }

      if (competitor.getStatus().equals(oldStatus)) {
        // the consumer class is in Worker, NewCompetitorsConsumer
        try {
          RabbitMQ.publishCompetitor(channel, getMQRoutingKey(), JsonConverter.toJson(competitor));
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        // the consumer class is here, StatusChangeConsumer
        StatusChange change = new StatusChange(competitor, oldStatus);
        try {
          RabbitMQ.publish(channel, SysProps.MQ_CHANGES_EXCHANGE(), SysProps.MQ_STATUS_CHANGES_ROUTING(), JsonConverter.toJson(change));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    RabbitMQ.closeChannel(channel);
  }

}
