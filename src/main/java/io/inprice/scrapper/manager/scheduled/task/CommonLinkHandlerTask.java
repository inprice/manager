package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;

import java.io.IOException;
import java.util.List;

public class CommonLinkHandlerTask extends AbstractLinkHandlerTask {

    private static final Logger log = new Logger(CommonLinkHandlerTask.class);

    public CommonLinkHandlerTask(LinkStatus status, String crontab, String queueName) {
        super(status, crontab, queueName);
    }

    @Override
    void handleLinks(List<Link> linksList) {
        for (Link link: linksList) {
            try {
                RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_LINK_EXCHANGE, getQueueName(), null, Converter.fromObject(link));
            } catch (IOException e) {
                boolean shouldBeStopped = incProblemCount(e);
                if (shouldBeStopped) break;
            }
        }
    }

}
