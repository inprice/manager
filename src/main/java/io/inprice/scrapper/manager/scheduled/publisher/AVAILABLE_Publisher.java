package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.config.Config;

/**
 * Finds and handles AVAILABLE links
 *
 * @author mdpinar
 */
public class AVAILABLE_Publisher extends AbstractLinkPublisher {

    public AVAILABLE_Publisher() {
        super(Status.AVAILABLE, Config.CRONTAB_FOR_AVAILABLE_LINKS, Config.RABBITMQ_AVAILABLE_LINKS_QUEUE);
    }

}
