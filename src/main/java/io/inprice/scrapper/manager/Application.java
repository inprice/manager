package io.inprice.scrapper.manager;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.manager.consumer.PriceChangeConsumer;
import io.inprice.scrapper.manager.consumer.StatusChangeConsumer;
import io.inprice.scrapper.manager.helpers.DBUtils;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.scheduled.TaskManager;

import java.io.IOException;
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
			Global.isRunning = true;

			TaskManager.start();
			StatusChangeConsumer.start();
			PriceChangeConsumer.start();

		}, "task-manager").start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Global.isRunning = false;

			TaskManager.stop();

			try {
				log.info("RabbitMQ connection is closing...");
				RabbitMQ.getChannel().abort();
				log.info("RabbitMQ is closed.");
			} catch (IOException e) {
				log.info("Rabbit abortion is interrupted.");
			}

			try {
				log.info("Thread pool is shutting down...");
				ThreadPools.MASTER_POOL.shutdown();
				ThreadPools.MASTER_POOL.awaitTermination(Config.WAITING_TIME_FOR_AWAIT_TERMINATION, TimeUnit.MILLISECONDS);
				log.info("Thread pool is shut down.");
			} catch (InterruptedException e) {
				log.info("Thread pool termination is interrupted.");
			}

			log.info("DB connection is closing...");
			DBUtils.shutdown();
			log.info("DB Connection is closed.");

			shutdown();
		}));
	}

	public static void shutdown() {
		log.info("TaskManager is shut down.");
	}

}
