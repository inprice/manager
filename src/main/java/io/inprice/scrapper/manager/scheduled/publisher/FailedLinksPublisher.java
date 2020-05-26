package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.external.Props;

abstract class FailedLinksPublisher extends AbstractLinkPublisher {

  abstract int getRetryLimit();

  List<Link> getLinks() {
    return linkRepository.getFailedLinks(getStatus(), getRetryLimit());
  }

  @Override
  String getMQRoutingKey() {
    return Props.MQ_ROUTING_FAILED_LINKS() + "." + getStatus().name();
  }

  @Override
  boolean isIncreaseRetry() {
    return true;
  }

}
