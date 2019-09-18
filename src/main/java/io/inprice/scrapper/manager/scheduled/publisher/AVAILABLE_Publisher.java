package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles AVAILABLE links
 *
 * @author mdpinar
 */
public class AVAILABLE_Publisher extends AbstractLinkPublisher {

    @Override
    Status getStatus() {
        return Status.AVAILABLE;
    }

    @Override
    String getMQRoutingKey() {
        return props.getRoutingKey_AvailableLinks();
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_AvailableLinks();
    }

}
