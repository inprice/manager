package io.inprice.scrapper.manager.scheduled.publishers;

import io.inprice.scrapper.common.meta.Status;

public class CommonLinkPublisher extends AbstractLinkPublisher {

    public CommonLinkPublisher(Status status, String cron, String queueName) {
        super(status, cron, queueName);
    }

}
