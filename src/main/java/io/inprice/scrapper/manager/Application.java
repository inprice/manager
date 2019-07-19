package io.inprice.scrapper.manager;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.manager.consumer.DeletedLinksConsumer;
import io.inprice.scrapper.manager.consumer.PriceChangeConsumer;
import io.inprice.scrapper.manager.consumer.StatusChangeConsumer;
import io.inprice.scrapper.manager.consumer.TobeAvailableLinksConsumer;
import io.inprice.scrapper.manager.helpers.*;
import io.inprice.scrapper.manager.scheduled.TaskManager;

/**
 * Entry point of the application.
 * 
 * @since 2019-04-20
 * @author mdpinar
 *
 */
public class Application {

	private static final Logger log = new Logger(Application.class);

	public static void main(String[] args) {
		new Thread(() -> {
			Global.isApplicationRunning = true;

			TaskManager.start();
			TobeAvailableLinksConsumer.start();
			StatusChangeConsumer.start();
			PriceChangeConsumer.start();
			DeletedLinksConsumer.start();

		}, "app-starter").start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("APPLICATION IS TERMINATING...");
			Global.isApplicationRunning = false;

			log.info(" - TaskManager scheduler is shutting down...");
			TaskManager.stop();

			log.info(" - Thread pools are shutting down...");
			ThreadPools.shutdown();

			log.info(" - Redis connection is closing...");
			RedisClient.shutdown();

			log.info(" - RabbitMQ connection is closing...");
			RabbitMQ.closeChannel();

			log.info(" - DB connection is closing...");
			DBUtils.shutdown();

			log.info("ALL SERVICES IS DONE.");
		},"shutdown-hook"));
	}

}
