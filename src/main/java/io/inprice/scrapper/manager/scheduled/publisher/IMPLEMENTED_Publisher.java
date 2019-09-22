package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;

/**
 * Finds and handles IMPLEMENTED links
 *
 * @author mdpinar
 */
public class IMPLEMENTED_Publisher extends NEW_Publisher {

    public IMPLEMENTED_Publisher() {
        super();
    }

    public IMPLEMENTED_Publisher(boolean lookForImportedProducts) {
        super(lookForImportedProducts);
    }

    @Override
    Status getStatus() {
        return Status.IMPLEMENTED;
    }

    @Override
    String getTimePeriodStatement() {
        return props.getTP_ImplementedLinks();
    }

}
