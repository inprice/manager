package io.inprice.crawler.manager;

import io.inprice.crawler.common.config.Config;
import io.inprice.crawler.common.helpers.RabbitMQ;
import io.inprice.crawler.common.helpers.ThreadPools;
import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.manager.helpers.DBUtils;
import io.inprice.crawler.manager.helpers.Global;
import io.inprice.crawler.manager.scheduled.TaskManager;

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
				ThreadPools.MASTER_POOL.awaitTermination(Config.TIME_FOR_TPOOL_TERMINATION, TimeUnit.MILLISECONDS);
				log.info("Thread pool is shut down.");
			} catch (InterruptedException e) {
				log.info("Thread pool termination is interrupted.");
			}

			log.info("DB connection is closing...");
			DBUtils.close();
			log.info("DB Connection is closed.");

			shutdown();
		}));
	}

	public static void shutdown() {
		log.info("TaskManager is shut down.");
	}

}
