package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.config.Config;

/**
 * Finds and handles NOT_AVAILABLE links
 *
 * @author mdpinar
 */
public class NOT_AVAILABLE_Publisher extends FailedLinksPublisher {

    public NOT_AVAILABLE_Publisher() {
        super(Status.NOT_AVAILABLE, Config.CRON_FOR_NOT_AVAILABLE_LINKS, Config.RETRY_LIMIT_FOR_FAILED_LINKS_G3);
    }

}
