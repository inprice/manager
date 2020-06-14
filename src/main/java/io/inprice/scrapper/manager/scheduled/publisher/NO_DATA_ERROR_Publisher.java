package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds and handles NO_DATA competitors
 *
 * @author mdpinar
 */
public class NO_DATA_ERROR_Publisher extends FailedCompetitorsPublisher {

  @Override
  CompetitorStatus getStatus() {
    return CompetitorStatus.NO_DATA;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_NO_DATA_ERRORS();
  }

  @Override
  int getRetryLimit() {
    return Props.RETRY_LIMIT_FOR_FAILED_COMPETITORS_G1();
  }

}
