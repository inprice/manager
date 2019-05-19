package io.inprice.scrapper.manager.scheduled;

import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.scheduled.publishers.CommonLinkPublisher;
import io.inprice.scrapper.manager.scheduled.publishers.FailedLinkPublisher;
import io.inprice.scrapper.manager.scheduled.publishers.NewLinkPublisher;
import io.inprice.scrapper.manager.scheduled.task.ProductPriceUpdater;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class TaskManager {

    private static final Logger log = new Logger(TaskManager.class);

    private static Scheduler scheduler;

    public static void start() {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            log.info("TaskManager is up.");

            loadTask(new ProductPriceUpdater());
            loadTask(new NewLinkPublisher());

            loadTask(
                new CommonLinkPublisher(
                    Status.ACTIVE,
                    Config.CRONTAB_FOR_ACTIVE_LINKS,
                    Config.RABBITMQ_ACTIVE_LINKS_QUEUE
                )
            );

            loadTask(
                new CommonLinkPublisher(
                    Status.SOCKET_ERROR,
                    Config.CRONTAB_FOR_SOCKET_ERRORS,
                    Config.RABBITMQ_FAILED_LINKS_QUEUE
                )
            );

            loadTask(
                new FailedLinkPublisher(
                    Status.NETWORK_ERROR,
                    Config.CRONTAB_FOR_NETWORK_ERRORS,
                    Config.RABBITMQ_FAILED_LINKS_QUEUE,
                    Config.RETRY_LIMIT_FOR_FAILED_LINKS_G1
                )
            );

            loadTask(
                new FailedLinkPublisher(
                    Status.UNAVAILABLE,
                    Config.CRONTAB_FOR_UNAVAILABLE_LINKS,
                    Config.RABBITMQ_FAILED_LINKS_QUEUE,
                    Config.RETRY_LIMIT_FOR_FAILED_LINKS_G3
                )
            );

        } catch (SchedulerException e) {
            log.error("Error in starting TaskManager up", e);
        }
    }

    private static void loadTask(Task task) throws SchedulerException {
        scheduler.scheduleJob(task.getJobDetail(), task.getTrigger());
    }

    public static void stop() {
        try {
            log.info("TaskManager is shutting down...");
            scheduler.shutdown(true);
            log.info("TaskManager is down.");
        } catch (SchedulerException e) {
            log.error("Error in stopping TaskManager", e);
        }
    }

}
