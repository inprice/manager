package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;

import java.util.List;

/**
 * This class finds links in RESUMED status and sends them to status change queue in Master project
 *
 * @author mdpinar
 */
public class ResumedLinksPublisher extends AbstractLinkPublisher {

    public ResumedLinksPublisher() {
        super(Status.RESUMED, Config.CRONTAB_FOR_RESUMED_LINKS, Config.RABBITMQ_STATUS_CHANGE_QUEUE);
    }

    @Override
    void handleLinks(List<Link> linkList) {
        for (Link link: linkList) {
            StatusChange change = new StatusChange(link);
            RabbitMQ.publish(Config.RABBITMQ_STATUS_CHANGE_QUEUE, Converter.fromObject(change)); //the consumer class is here, StatusChangeConsumer
        }
    }

}
