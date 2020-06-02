package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.models.Competitor;

abstract class FailedCompetitorsPublisher extends AbstractCompetitorPublisher {

  abstract int getRetryLimit();

  List<Competitor> getCompetitors() {
    return competitorRepository.getFailedCompetitors(getStatus(), getRetryLimit());
  }

  @Override
  String getMQRoutingKey() {
    return SysProps.MQ_FAILED_COMPETITORS_ROUTING() + "." + getStatus().name();
  }

  @Override
  boolean isIncreaseRetry() {
    return true;
  }

}
