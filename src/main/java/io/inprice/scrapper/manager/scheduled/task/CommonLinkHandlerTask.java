package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.common.meta.LinkStatus;

public class CommonLinkHandlerTask extends AbstractLinkHandlerTask {

    public CommonLinkHandlerTask(LinkStatus status, String crontab, String queueName) {
        super(status, crontab, queueName);
    }

}
