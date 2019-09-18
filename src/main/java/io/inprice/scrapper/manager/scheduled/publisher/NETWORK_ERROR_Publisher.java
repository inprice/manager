package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles NETWORK_ERROR links
 *
 * @author mdpinar
 */
public class NETWORK_ERROR_Publisher extends FailedLinksPublisher {

    public NETWORK_ERROR_Publisher() {
        super(props.getRL_FailedLinksG1());
    }

    @Override
    Status getStatus() {
        return Status.NETWORK_ERROR;
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_NetworkErrors();
    }

}
