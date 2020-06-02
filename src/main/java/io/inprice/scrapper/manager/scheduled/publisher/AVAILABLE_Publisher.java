package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds and handles AVAILABLE competitors
 *
 * @author mdpinar
 */
public class AVAILABLE_Publisher extends AbstractCompetitorPublisher {

  @Override
  CompetitorStatus getStatus() {
    return CompetitorStatus.AVAILABLE;
  }

  @Override
  String getMQRoutingKey() {
    return SysProps.MQ_AVAILABLE_COMPETITORS_ROUTING();
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_AVAILABLE_COMPETITORS();
  }

}
