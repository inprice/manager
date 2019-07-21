package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.config.Config;

/**
 * Finds and handles RENEWED links
 *
 * @author mdpinar
 */
public class RENEWED_Publisher extends NEW_Publisher {

    public RENEWED_Publisher() {
        super(Status.RENEWED, Config.CRON_FOR_RENEWED_LINKS, Config.MQ_NEW_LINKS_QUEUE);
    }

}
