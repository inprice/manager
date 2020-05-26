package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.manager.external.Props;

/**
 * Finds and handles SOCKET_ERROR links
 *
 * @author mdpinar
 */
public class SOCKET_ERROR_Publisher extends FailedLinksPublisher {

  @Override
  LinkStatus getStatus() {
    return LinkStatus.SOCKET_ERROR;
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_SOCKET_ERRORS();
  }

  @Override
  int getRetryLimit() {
    return Props.RETRY_LIMIT_FOR_FAILED_LINKS_G3();
  }

}
