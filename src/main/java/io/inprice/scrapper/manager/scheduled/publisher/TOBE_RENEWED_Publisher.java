package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds and handles RENEWED competitors
 *
 * @author mdpinar
 */
public class TOBE_RENEWED_Publisher extends TOBE_CLASSIFIED_Publisher {

  @Override
  CompetitorStatus getStatus() {
    return CompetitorStatus.TOBE_RENEWED;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_TOBE_RENEWED_COMPETITORS();
  }

}
