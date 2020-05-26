package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.manager.external.Props;

/**
 * Finds and handles IMPLEMENTED links
 *
 * @author mdpinar
 */
public class IMPLEMENTED_Publisher extends NEW_Publisher {

  @Override
  LinkStatus getStatus() {
    return LinkStatus.IMPLEMENTED;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_IMPLEMENTED_LINKS();
  }

}
