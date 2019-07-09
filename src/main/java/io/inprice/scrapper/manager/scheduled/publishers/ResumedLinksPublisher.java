package io.inprice.scrapper.manager.scheduled.publishers;

import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;

import java.io.IOException;
import java.util.List;

/**
 * This class finds links in RESUMED status and publishes to set their statuses to previous status
 */
public class ResumedLinksPublisher extends AbstractLinkPublisher {

    public ResumedLinksPublisher() {
        super(Status.RESUMED, Config.CRONTAB_FOR_RESUMED_LINKS, Config.RABBITMQ_STATUS_CHANGE_QUEUE);
    }

    @Override
    void handleLinks(List<Link> linkList) {
        for (Link link: linkList) {
            try {
                StatusChange change = new StatusChange(link, link.getPreviousStatus());
                RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_LINK_EXCHANGE, getQueueName(), null, Converter.fromObject(change));
            } catch (IOException e) {
                boolean shouldBeStopped = incProblemCount(e);
                if (shouldBeStopped) break;
            }
        }
    }

}
