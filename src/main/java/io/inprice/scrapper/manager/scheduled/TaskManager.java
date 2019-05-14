package io.inprice.scrapper.manager.scheduled;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.manager.scheduled.task.CommonLinkHandlerTask;
import io.inprice.scrapper.manager.scheduled.task.NewLinkHandlerTask;
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

            loadTask(new NewLinkHandlerTask());
            loadTask(new CommonLinkHandlerTask(LinkStatus.ACTIVE, Config.CRONTAB_FOR_ACTIVE_LINKS, Config.RABBITMQ_ACTIVE_LINKS_QUEUE));
            loadTask(new CommonLinkHandlerTask(LinkStatus.ACTIVE, Config.CRONTAB_FOR_SOCKET_ERRORS, Config.RABBITMQ_SOCKET_ERRORS_QUEUE));

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
