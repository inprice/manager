package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.config.Config;

/**
 * Finds and handles SOCKET_ERROR links
 *
 * @author mdpinar
 */
public class SOCKET_ERROR_Publisher extends FailedLinksPublisher {

    public SOCKET_ERROR_Publisher() {
        super(Status.SOCKET_ERROR, Config.CRONTAB_FOR_SOCKET_ERRORS, Config.RETRY_LIMIT_FOR_FAILED_LINKS_G3);
    }

}
