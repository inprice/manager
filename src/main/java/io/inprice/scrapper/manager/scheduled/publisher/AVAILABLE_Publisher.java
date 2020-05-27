package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.manager.config.Props;

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
    return SysProps.MQ_AVAILABLE_LINKS_ROUTING();
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_AVAILABLE_LINKS();
  }

}
