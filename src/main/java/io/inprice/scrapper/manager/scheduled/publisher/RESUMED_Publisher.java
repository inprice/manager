package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;

import java.util.List;

/**
 * Finds links in RESUMED status and changes back to their previous status
 *
 * @author mdpinar
 */
public class RESUMED_Publisher extends AbstractLinkPublisher {

    public RESUMED_Publisher() {
        super(Status.RESUMED, Config.CRON_FOR_RESUMED_LINKS, Config.MQ_STATUS_CHANGE_QUEUE);
    }

    @Override
    void handleLinks(List<Link> linkList) {
        for (Link link: linkList) {
            StatusChange change = new StatusChange(link, Status.RESUMED);
            RabbitMQ.publish(Config.MQ_STATUS_CHANGE_QUEUE, change); //the consumer class is here, StatusChangeConsumer
        }
    }

}
