package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds and handles NOT_AVAILABLE links
 *
 * @author mdpinar
 */
public class NOT_AVAILABLE_Publisher extends FailedLinksPublisher {

  @Override
  LinkStatus getStatus() {
    return LinkStatus.NOT_AVAILABLE;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_NOT_AVAILABLE_LINKS();
  }

  @Override
  int getRetryLimit() {
    return Props.RETRY_LIMIT_FOR_FAILED_LINKS_G3();
  }

}
