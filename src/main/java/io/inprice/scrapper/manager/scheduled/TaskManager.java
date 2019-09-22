package io.inprice.scrapper.manager.scheduled;

import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.manager.scheduled.publisher.*;
import io.inprice.scrapper.manager.scheduled.updater.PriceUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaskManager {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(15);

    public static void start() {
        log.info("TaskManager is up.");

        loadTask(new PriceUpdater());

        loadTask(new NEW_Publisher());
        loadTask(new RENEWED_Publisher());
        loadTask(new IMPLEMENTED_Publisher());
        loadTask(new RESUMED_Publisher());
        loadTask(new AVAILABLE_Publisher());
        loadTask(new NETWORK_ERROR_Publisher());
        loadTask(new SOCKET_ERROR_Publisher());
        loadTask(new NOT_AVAILABLE_Publisher());

        //tasks for imported products
        loadTask(new NEW_Publisher(true));
        loadTask(new IMPLEMENTED_Publisher(true));
        loadTask(new NETWORK_ERROR_Publisher(true));
        loadTask(new SOCKET_ERROR_Publisher(true));
    }

    private static void loadTask(Task task) {
        TimePeriod tp = task.getTimePeriod();
        scheduler.scheduleAtFixedRate(task, 0, tp.getInterval(), tp.getTimeUnit());
    }

    public static void stop() {
        try {
            scheduler.shutdown();
        } catch (SecurityException e) {
            log.error("Failed to stop TaskManager's scheduler.", e);
        }
    }

}
