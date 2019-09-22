package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles NETWORK_ERROR links
 *
 * @author mdpinar
 */
public class NETWORK_ERROR_Publisher extends FailedLinksPublisher {

    public NETWORK_ERROR_Publisher() {
        super();
    }

    public NETWORK_ERROR_Publisher(boolean lookForImportedProducts) {
        super(lookForImportedProducts);
    }

    @Override
    Status getStatus() {
        return Status.NETWORK_ERROR;
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_NetworkErrors();
    }

    @Override
    int getRetryLimit() {
        return props.getRL_FailedLinksG1();
    }

}
