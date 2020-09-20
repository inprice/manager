package io.inprice.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.StatusChange;
import io.inprice.common.meta.CompetitorStatus;
import io.inprice.common.models.Competitor;
import io.inprice.common.models.Site;
import io.inprice.common.utils.URLUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.helpers.SiteFinder;

/**
 * Finds and handles NEW (and also RENEWED through inheritance) competitors
 *
 * @author mdpinar
 */
public class TOBE_CLASSIFIED_Publisher extends AbstractCompetitorPublisher {

  private static final Logger log = LoggerFactory.getLogger(TOBE_CLASSIFIED_Publisher.class);

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
          // if site is in a status then each competitor pointing this site should be the same status
          if (site.getStatus() != null) competitor.setStatus(CompetitorStatus.valueOf(site.getStatus()));
        } else {
          competitor.setStatus(CompetitorStatus.TOBE_IMPLEMENTED);
        }
      } else {
        competitor.setStatus(CompetitorStatus.IMPROPER);
      }

      if (competitor.getStatus().equals(oldStatus)) {
        // the consumer class is in Worker, TobeClassifiedCompetitorsConsumer
        try {
          RabbitMQ.publishCompetitor(channel, getMQRoutingKey(), JsonConverter.toJson(competitor));
        } catch (Exception e) {
          log.error("Failed to publish tobe classified competitor. Case 1", e);
        }
      } else {
        // the consumer class is here, StatusChangeConsumer
        StatusChange change = new StatusChange(competitor, oldStatus);
        try {
          RabbitMQ.publish(channel, SysProps.MQ_CHANGES_EXCHANGE(), SysProps.MQ_STATUS_CHANGES_ROUTING(), JsonConverter.toJson(change));
        } catch (Exception e) {
          log.error("Failed to publish tobe classified competitor. Case 2", e);
        }
      }
    }
    RabbitMQ.closeChannel(channel);
  }

}
