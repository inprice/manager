package io.inprice.scrapper.manager;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.consumer.AvailableLinksConsumer;
import io.inprice.scrapper.manager.consumer.LinkPriceChangeConsumer;
import io.inprice.scrapper.manager.consumer.StatusChangeConsumer;
import io.inprice.scrapper.manager.helpers.*;
import io.inprice.scrapper.manager.scheduled.TaskManager;

import java.util.concurrent.TimeUnit;

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
			AvailableLinksConsumer.start();
			StatusChangeConsumer.start();
			LinkPriceChangeConsumer.start();

		}, "publisher-manager").start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Global.isApplicationRunning = false;

			TaskManager.stop();

			log.info("Redis connection is shutting down...");
			RedisClient.shutdown();
			log.info("Redis is shutdown.");

			log.info("RabbitMQ connection is closing...");
			RabbitMQ.closeChannel();
			log.info("RabbitMQ is closed.");

			try {
				log.info("Thread pool is shutting down...");
				ThreadPools.MASTER_POOL.shutdown();
				ThreadPools.MASTER_POOL.awaitTermination(Config.WAITING_TIME_FOR_AWAIT_TERMINATION, TimeUnit.MILLISECONDS);
				log.info("Thread pool is shutdown.");
			} catch (InterruptedException e) {
				log.info("Thread pool termination is interrupted.");
			}

			log.info("DB connection is closing...");
			DBUtils.shutdown();
			log.info("DB Connection is closed.");

			shutdown();
		}));
	}

	private static void shutdown() {
		log.info("TaskManager is shutdown.");
	}

}
