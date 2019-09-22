package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles SOCKET_ERROR links
 *
 * @author mdpinar
 */
public class SOCKET_ERROR_Publisher extends FailedLinksPublisher {

    public SOCKET_ERROR_Publisher() {
        super();
    }

    public SOCKET_ERROR_Publisher(boolean lookForImportedProducts) {
        super(lookForImportedProducts);
    }

    @Override
    Status getStatus() {
        return Status.SOCKET_ERROR;
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_SocketErrors();
    }

    @Override
    int getRetryLimit() {
        return props.getRL_FailedLinksG3();
    }

}
