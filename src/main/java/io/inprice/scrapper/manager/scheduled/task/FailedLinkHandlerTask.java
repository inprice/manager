package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.repository.Links;

import java.util.List;

public class FailedLinkHandlerTask extends AbstractLinkHandlerTask {

    public FailedLinkHandlerTask(LinkStatus status, String crontab, String queueName, int retryLimit) {
        super(status, crontab, queueName, true, retryLimit);
    }

    List<Link> getLinks() {
        return Links.getFailedLinks(getLinkStatus(), getCycle(), getRetryLimit());
    }

}
