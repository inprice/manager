package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles SOCKET_ERROR links
 *
 * @author mdpinar
 */
public class SOCKET_ERROR_Publisher extends FailedLinksPublisher {

    public SOCKET_ERROR_Publisher() {
        super(props.getRL_FailedLinksG3());
    }

    @Override
    Status getStatus() {
        return Status.SOCKET_ERROR;
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_SocketErrors();
    }

}
