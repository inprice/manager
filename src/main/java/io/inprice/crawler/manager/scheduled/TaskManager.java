package io.inprice.crawler.manager.scheduled;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.manager.scheduled.task.PopulateActiveSitesTask;
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
            loadTask(new PopulateActiveSitesTask());
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
