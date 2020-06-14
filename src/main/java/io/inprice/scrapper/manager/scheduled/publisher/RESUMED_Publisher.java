package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.JsonConverter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.common.models.Competitor;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds competitors in RESUMED status and changes back to their previous status
 *
 * @author mdpinar
 */
public class RESUMED_Publisher extends AbstractCompetitorPublisher {

  private static final Logger log = LoggerFactory.getLogger(RESUMED_Publisher.class);

  @Override
  CompetitorStatus getStatus() {
    return CompetitorStatus.RESUMED;
  }

  @Override
  String getMQRoutingKey() {
    return SysProps.MQ_STATUS_CHANGES_ROUTING();
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_RESUMED_COMPETITORS();
  }

  @Override
  void handleCompetitors(List<Competitor> competitorList) {
    Channel channel = RabbitMQ.openChannel();
    try {
      for (Competitor competitor : competitorList) {
        StatusChange change = new StatusChange(competitor, getStatus());
        RabbitMQ.publish(channel, SysProps.MQ_CHANGES_EXCHANGE(), getMQRoutingKey(), JsonConverter.toJson(change));
      }
    } catch (Exception e) {
      log.error("Failed to hande resumed competitors", e);
    }
    RabbitMQ.closeChannel(channel);
  }

}
