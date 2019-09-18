package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.repository.Links;

import java.util.List;

abstract class FailedLinksPublisher extends AbstractLinkPublisher {

    private int retryLimit;

    FailedLinksPublisher(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    List<Link> getLinks() {
        return Links.getFailedLinks(getStatus(), this.retryLimit);
    }

    @Override
    String getMQRoutingKey() {
        return props.getRoutingKey_FailedLinks();
    }

    @Override
    boolean isIncreaseRetry() {
        return true;
    }
}
