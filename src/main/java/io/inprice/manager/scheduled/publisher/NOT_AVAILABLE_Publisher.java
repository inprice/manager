package io.inprice.manager.scheduled.publisher;

import io.inprice.common.meta.CompetitorStatus;
import io.inprice.manager.config.Props;

/**
 * Finds and handles NOT_AVAILABLE competitors
 *
 * @author mdpinar
 */
public class NOT_AVAILABLE_Publisher extends FailedCompetitorsPublisher {

  @Override
  CompetitorStatus getStatus() {
    return CompetitorStatus.NOT_AVAILABLE;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_NOT_AVAILABLE_COMPETITORS();
  }

  @Override
  int getRetryLimit() {
    return Props.RETRY_LIMIT_FOR_FAILED_COMPETITORS_G3();
  }

}
