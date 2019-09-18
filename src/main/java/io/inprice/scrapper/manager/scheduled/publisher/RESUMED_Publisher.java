package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.RabbitMQ;

import java.util.List;

/**
 * Finds links in RESUMED status and changes back to their previous status
 *
 * @author mdpinar
 */
public class RESUMED_Publisher extends AbstractLinkPublisher {

    @Override
    Status getStatus() {
        return Status.RESUMED;
    }

    @Override
    String getMQRoutingKey() {
        return props.getRoutingKey_StatusChange();
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_ResumedLinks();
    }

    @Override
    void handleLinks(List<Link> linkList) {
        for (Link link: linkList) {
            StatusChange change = new StatusChange(link, getStatus());
            RabbitMQ.publish(getMQRoutingKey(), change); //the consumer class is here, StatusChangeConsumer
        }
    }

}
