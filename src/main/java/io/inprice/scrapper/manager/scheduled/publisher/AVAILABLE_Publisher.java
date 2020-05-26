package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.manager.external.Props;

/**
 * Finds and handles AVAILABLE links
 *
 * @author mdpinar
 */
public class AVAILABLE_Publisher extends AbstractLinkPublisher {

  @Override
  LinkStatus getStatus() {
    return LinkStatus.AVAILABLE;
  }

  @Override
  String getMQRoutingKey() {
    return Props.MQ_ROUTING_AVAILABLE_LINKS();
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_AVAILABLE_LINKS();
  }

}
