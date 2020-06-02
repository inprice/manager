package io.inprice.scrapper.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.manager.consumer.PriceRefreshConsumer;
import io.inprice.scrapper.manager.consumer.PriceChangeConsumer;
import io.inprice.scrapper.manager.consumer.StatusChangeConsumer;
import io.inprice.scrapper.manager.consumer.TobeAvailableCompetitorsConsumer;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.helpers.ThreadPools;
import io.inprice.scrapper.manager.scheduled.TaskManager;

/**
 * Entry point of the application.
 * 
 * @since 2019-04-20
 * @author mdpinar
 *
 */
public class Application {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

  private static final Database db = Beans.getSingleton(Database.class);

	public static void main(String[] args) {
		new Thread(() -> {
			Global.isApplicationRunning = true;

			TaskManager.start();
			TobeAvailableCompetitorsConsumer.start();
			StatusChangeConsumer.start();
			PriceChangeConsumer.start();
			PriceRefreshConsumer.start();

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
			RabbitMQ.closeConnection();

			log.info(" - DB connection is closing...");
			db.shutdown();

			log.info("ALL SERVICES IS DONE.");
		},"shutdown-hook"));
	}

}
