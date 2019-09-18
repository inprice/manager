package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles NOT_AVAILABLE links
 *
 * @author mdpinar
 */
public class NOT_AVAILABLE_Publisher extends FailedLinksPublisher {

    public NOT_AVAILABLE_Publisher() {
        super(props.getRL_FailedLinksG3());
    }

    @Override
    Status getStatus() {
        return Status.NOT_AVAILABLE;
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_NotAvailableLinks();
    }

}
