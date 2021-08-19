package io.inprice.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.manager.config.Props;
import io.inprice.manager.consumer.ConsumerManager;
import io.inprice.manager.helpers.Global;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Entry point of the application.
 * 
 * @since 2019-04-20
 * @author mdpinar
 *
 */
public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		new Thread(() -> {
			Global.isApplicationRunning = true;

      Database.start(Props.getConfig().MYSQL_CONF);
      logger.info(" - Connected to Mysql server.");

      RabbitMQ.start(Props.getConfig().RABBIT_CONF);
      logger.info(" - Connected to RabbitMQ server.");

			TaskManager.start();

			ConsumerManager.start();

		}, "app-starter").start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("APPLICATION IS TERMINATING...");
			Global.isApplicationRunning = false;

			logger.info(" - TaskManager is shutting down...");
			TaskManager.stop();

      logger.info(" - RabbitMQ connection is closing...");
      RabbitMQ.stop();

      logger.info(" - Mysql connection is closing...");
      Database.stop();

			logger.info("ALL SERVICES IS DONE.");

    }, "shutdown-hook"));
	}

}
