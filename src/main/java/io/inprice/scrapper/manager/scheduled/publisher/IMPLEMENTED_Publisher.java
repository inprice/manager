package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles IMPLEMENTED links
 *
 * @author mdpinar
 */
public class IMPLEMENTED_Publisher extends NEW_Publisher {

    @Override
    Status getStatus() {
        return Status.IMPLEMENTED;
    }

    @Override
    String getMQRoutingKey() {
        return props.getRoutingKey_NewLinks();
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_ImplementedLinks();
    }

}
