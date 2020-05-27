package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds and handles RENEWED links
 *
 * @author mdpinar
 */
public class RENEWED_Publisher extends NEW_Publisher {

  @Override
  LinkStatus getStatus() {
    return LinkStatus.RENEWED;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_RENEWED_LINKS();
  }

}
