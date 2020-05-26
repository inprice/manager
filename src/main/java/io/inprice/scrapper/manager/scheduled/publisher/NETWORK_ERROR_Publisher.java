package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.manager.external.Props;

/**
 * Finds and handles NETWORK_ERROR links
 *
 * @author mdpinar
 */
public class NETWORK_ERROR_Publisher extends FailedLinksPublisher {

  @Override
  LinkStatus getStatus() {
    return LinkStatus.NETWORK_ERROR;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_NETWORK_ERRORS();
  }

  @Override
  int getRetryLimit() {
    return Props.RETRY_LIMIT_FOR_FAILED_LINKS_G1();
  }

}
