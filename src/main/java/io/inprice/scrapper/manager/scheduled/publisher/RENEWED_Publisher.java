package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles RENEWED links
 *
 * @author mdpinar
 */
public class RENEWED_Publisher extends NEW_Publisher {

    @Override
    Status getStatus() {
        return Status.RENEWED;
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_RenewedLinks();
    }

}
