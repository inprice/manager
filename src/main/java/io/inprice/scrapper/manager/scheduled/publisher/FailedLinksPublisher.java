package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.repository.Links;

import java.util.List;

public class FailedLinksPublisher extends AbstractLinkPublisher {

    private int retryLimit;

    public FailedLinksPublisher(Status status, String cron, int retryLimit) {
        super(status, cron, Config.RABBITMQ_FAILED_LINKS_QUEUE);
        this.retryLimit = retryLimit;
    }

    List<Link> getLinks() {
        return Links.getFailedLinks(getStatus(), this.retryLimit);
    }

}
