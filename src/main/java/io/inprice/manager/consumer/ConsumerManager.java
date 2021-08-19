package io.inprice.manager.consumer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.QueueDef;
import io.inprice.manager.config.Props;

public class ConsumerManager {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerManager.class);

  public static void start() {
    logger.info("Consumer manager is starting...");

    try {
	  	QueueDef sendingEmailsQueue = Props.getConfig().QUEUES.SENDING_EMAILS;
	  	QueueDef statusChangingLinksQueue = Props.getConfig().QUEUES.STATUS_CHANGING_LINKS;
	
	  	if (sendingEmailsQueue.ACTIVE) new SendingEmailsConsumer(sendingEmailsQueue);
	  	if (statusChangingLinksQueue.ACTIVE) new StatusChangingLinksConsumer(statusChangingLinksQueue);
	
			logger.info("Consumer manager is started.");
		} catch (IOException e) {
			logger.error("Failed to start consumer manager", e);
		}
  }

}
