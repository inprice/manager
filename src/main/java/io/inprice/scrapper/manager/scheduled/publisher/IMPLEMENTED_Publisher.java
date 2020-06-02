package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds and handles IMPLEMENTED competitors
 *
 * @author mdpinar
 */
public class IMPLEMENTED_Publisher extends TOBE_CLASSIFIED_Publisher {

  @Override
  CompetitorStatus getStatus() {
    return CompetitorStatus.IMPLEMENTED;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_IMPLEMENTED_COMPETITORS();
  }

}
