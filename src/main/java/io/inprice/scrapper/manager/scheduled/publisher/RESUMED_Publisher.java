package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Props;

/**
 * Finds links in RESUMED status and changes back to their previous status
 *
 * @author mdpinar
 */
public class RESUMED_Publisher extends AbstractLinkPublisher {

  @Override
  LinkStatus getStatus() {
    return LinkStatus.RESUMED;
  }

  @Override
  String getMQRoutingKey() {
    return SysProps.MQ_STATUS_CHANGES_ROUTING();
  }

  @Override
  String getTimePeriodStatement() {
    return Props.TIMING_FOR_RESUMED_LINKS();
  }

  @Override
  void handleLinks(List<Link> linkList) {
    for (Link link : linkList) {
      StatusChange change = new StatusChange(link, getStatus());
      RabbitMQ.publish(getMQRoutingKey(), change); // the consumer class is here, StatusChangeConsumer
    }
  }

}
