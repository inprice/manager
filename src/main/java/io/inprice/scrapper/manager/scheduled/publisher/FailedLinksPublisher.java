package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.models.Link;

abstract class FailedLinksPublisher extends AbstractLinkPublisher {

  abstract int getRetryLimit();

  List<Link> getLinks() {
    return linkRepository.getFailedLinks(getStatus(), getRetryLimit());
  }

  @Override
  String getMQRoutingKey() {
    return SysProps.MQ_FAILED_LINKS_ROUTING() + "." + getStatus().name();
  }

  @Override
  boolean isIncreaseRetry() {
    return true;
  }

}
