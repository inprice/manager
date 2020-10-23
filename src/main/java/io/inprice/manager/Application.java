package io.inprice.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.manager.consumer.ParsedLinksConsumer;
import io.inprice.manager.helpers.Global;
import io.inprice.manager.helpers.RedisClient;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Entry point of the application.
 * 
 * @since 2019-04-20
 * @author mdpinar
 *
 */
public class Application {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		new Thread(() -> {
			Global.isApplicationRunning = true;

			TaskManager.start();
			ParsedLinksConsumer.start();

		}, "app-starter").start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("APPLICATION IS TERMINATING...");
			Global.isApplicationRunning = false;

			log.info(" - TaskManager is shutting down...");
			TaskManager.stop();

			log.info(" - ParsedLinksConsumer is shutting down...");
			ParsedLinksConsumer.stop();

			log.info(" - Redis connection is closing...");
			RedisClient.shutdown();

      log.info(" - DB connection is closing...");
      Database.shutdown();

			log.info("ALL SERVICES IS DONE.");

    }, "shutdown-hook"));
	}

}
