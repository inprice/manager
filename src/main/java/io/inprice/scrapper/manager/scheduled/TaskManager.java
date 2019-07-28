package io.inprice.scrapper.manager.scheduled;

import io.inprice.scrapper.manager.scheduled.publisher.AVAILABLE_Publisher;
import io.inprice.scrapper.manager.scheduled.publisher.RENEWED_Publisher;
import io.inprice.scrapper.manager.scheduled.updater.PriceUpdater;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskManager {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

    private static Scheduler scheduler;

    public static void start() {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();

            log.info("TaskManager is up.");

            loadTask(new PriceUpdater());

//            loadTask(new NEW_Publisher());
            loadTask(new RENEWED_Publisher());
//            loadTask(new IMPLEMENTED_Publisher()); ???
//            loadTask(new RESUMED_Publisher()); ???
            loadTask(new AVAILABLE_Publisher());
//            loadTask(new NETWORK_ERROR_Publisher());
//            loadTask(new SOCKET_ERROR_Publisher());
//            loadTask(new NOT_AVAILABLE_Publisher());

        } catch (SchedulerException e) {
            log.error("Failed to start TaskManager's scheduler.", e);
        }
    }

    private static void loadTask(Task task) throws SchedulerException {
        scheduler.scheduleJob(task.getJobDetail(), task.getTrigger());
    }

    public static void stop() {
        try {
            scheduler.shutdown(true);
        } catch (SchedulerException e) {
            log.error("Failed to stop TaskManager's scheduler.", e);
        }
    }

}
