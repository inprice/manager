package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds and handles NETWORK_ERROR competitors
 *
 * @author mdpinar
 */
public class NETWORK_ERROR_Publisher extends FailedCompetitorsPublisher {

  @Override
  CompetitorStatus getStatus() {
    return CompetitorStatus.NETWORK_ERROR;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_NETWORK_ERRORS();
  }

  @Override
  int getRetryLimit() {
    return Props.RETRY_LIMIT_FOR_FAILED_COMPETITORS_G1();
  }

}
