package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.config.Config;

/**
 * Finds and handles NETWORK_ERROR links
 *
 * @author mdpinar
 */
public class NETWORK_ERROR_Publisher extends FailedLinksPublisher {

    public NETWORK_ERROR_Publisher() {
        super(Status.NETWORK_ERROR, Config.CRON_FOR_NETWORK_ERRORS, Config.RETRY_LIMIT_FOR_FAILED_LINKS_G1);
    }

}
