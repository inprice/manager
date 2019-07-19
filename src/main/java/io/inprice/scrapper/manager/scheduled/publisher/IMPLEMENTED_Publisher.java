package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.config.Config;

/**
 * Finds and handles IMPLEMENTED links
 *
 * @author mdpinar
 */
public class IMPLEMENTED_Publisher extends NEW_Publisher {

    public IMPLEMENTED_Publisher() {
        super(Status.IMPLEMENTED, Config.CRONTAB_FOR_IMPLEMENTED_LINKS, Config.RABBITMQ_NEW_LINKS_QUEUE);
    }

}
