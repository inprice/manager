package io.inprice.scrapper.manager.scheduled;

import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.scheduled.publishers.CommonLinkPublisher;
import io.inprice.scrapper.manager.scheduled.publishers.FailedLinksPublisher;
import io.inprice.scrapper.manager.scheduled.publishers.NewLinksPublisher;
import io.inprice.scrapper.manager.scheduled.publishers.ResumedLinksPublisher;
import io.inprice.scrapper.manager.scheduled.tasks.ProductPriceUpdater;
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

            loadTask(new NewLinksPublisher());
            loadTask(new ResumedLinksPublisher());

            loadTask(
                new CommonLinkPublisher(
                    Status.AVAILABLE,
                    Config.CRONTAB_FOR_AVAILABLE_LINKS,
                    Config.RABBITMQ_AVAILABLE_LINKS_QUEUE
                )
            );

            loadTask(
                new CommonLinkPublisher(
                    Status.RENEWED,
                    Config.CRONTAB_FOR_RENEWED_LINKS,
                    Config.RABBITMQ_AVAILABLE_LINKS_QUEUE
                )
            );

            loadTask(
                new FailedLinksPublisher(
                    Status.NETWORK_ERROR,
                    Config.CRONTAB_FOR_NETWORK_ERRORS,
                    Config.RABBITMQ_FAILED_LINKS_QUEUE,
                    Config.RETRY_LIMIT_FOR_FAILED_LINKS_G1
                )
            );

            loadTask(
                new FailedLinksPublisher(
                    Status.SOCKET_ERROR,
                    Config.CRONTAB_FOR_SOCKET_ERRORS,
                    Config.RABBITMQ_FAILED_LINKS_QUEUE,
                    Config.RETRY_LIMIT_FOR_FAILED_LINKS_G2
                )
            );

            loadTask(
                new FailedLinksPublisher(
                    Status.OUT_OF_STOCK,
                    Config.CRONTAB_FOR_OUT_OF_STOCK_LINKS,
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
