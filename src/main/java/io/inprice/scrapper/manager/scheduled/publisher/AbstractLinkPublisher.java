package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.util.List;

/**
 * NEW LINKS, ACTIVE LINKS and the links which have SOCKET_ERROR are handled by this publisher
 */
public abstract class AbstractLinkPublisher implements Task {

    private static final Logger log = new Logger("LinkHandlerTask");

    private final Status status;
    private final String crontab;
    private final String queueName;

    AbstractLinkPublisher(Status status, String crontab, String queueName) {
        this.status = status;
        this.crontab = crontab;
        this.queueName = queueName;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        synchronized (log) {
            if (Global.isTaskRunning(status.name())) {
                log.warn("%s Links Handler is already triggered and hasn't finished yet!", status);
                return;
            }
        }

        Global.setTaskRunningStatus(status.name(), true);
        int counter = 0;

        log.info("Link handler for %s status is starting...", status);

        List<Link> links = getLinks();
        if (links.size() == 0) {
            log.info("There is no suitable link in %s status.", status);
        } else {

            counter += links.size();

            while (Global.isApplicationRunning && links.size() > 0) {
                log.info("%d of %s links are being handled...", links.size(), status);

                handleLinks(links);

                if (links.size() == Config.DB_FETCH_LIMIT) {
                    try {
                        Thread.sleep(Config.WAITING_TIME_FOR_GETTING_LINKS_FROM_DB);
                    } catch (InterruptedException e) {
                        //
                    }
                }

                if (Global.isApplicationRunning) links = getLinks();
                counter += links.size();
            }
        }
        log.info("Link Handler for %s status is completed. Total link count: %d", status, counter);

        Global.setTaskRunningStatus(status.name(), false);
    }

    void handleLinks(List<Link> linkList) {
        for (Link link: linkList) {
            RabbitMQ.publish(queueName, link);
        }
    }

    List<Link> getLinks() {
        return Links.getLinks(getStatus());
    }

    @Override
    public Trigger getTrigger() {
        return TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder.cronSchedule(crontab)
            )
        .build();
    }

    @Override
    public JobDetail getJobDetail() {
        return JobBuilder.newJob(getClass()).build();
    }

    Status getStatus() {
        return status;
    }

}
