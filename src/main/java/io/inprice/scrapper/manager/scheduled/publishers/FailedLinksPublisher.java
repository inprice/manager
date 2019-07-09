package io.inprice.scrapper.manager.scheduled.publishers;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.repository.Links;

import java.util.List;

public class FailedLinksPublisher extends AbstractLinkPublisher {

    public FailedLinksPublisher(Status status, String cron, String queueName, int retryLimit) {
        super(status, cron, queueName, true, retryLimit);
    }

    List<Link> getLinks() {
        return Links.getFailedLinks(getStatus(), getCycle(), getRetryLimit());
    }

}
