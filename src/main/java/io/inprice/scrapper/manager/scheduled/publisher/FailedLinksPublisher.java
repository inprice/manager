package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.models.Link;

import java.util.List;

abstract class FailedLinksPublisher extends AbstractLinkPublisher {

    abstract int getRetryLimit();

    public FailedLinksPublisher() {
        super();
    }

    public FailedLinksPublisher(boolean lookForImportedProducts) {
        super(lookForImportedProducts);
    }

    List<Link> getLinks() {
        return linkRepository.getFailedLinks(getStatus(), getRetryLimit(), isLookingForImportedProducts());
    }

    @Override
    String getMQRoutingKey() {
        return props.getRoutingKey_FailedLinks() + "." + getStatus().name();
    }

    @Override
    boolean isIncreaseRetry() {
        return true;
    }
}
