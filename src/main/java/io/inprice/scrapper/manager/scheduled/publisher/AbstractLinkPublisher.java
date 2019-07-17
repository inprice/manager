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
 * This is a class containing common functions used by all the publishers.
 * Since quartz needs to create publisher classes through their default (without args) constructors,
 * in each publisher, please add a default constructor referencing this class's constructors.
 *
 * @author mdpinar
 */
public abstract class AbstractLinkPublisher implements Task {

    private static final Logger log = new Logger("LinkHandlerTask");

    private final Status status;
    private final String crontab;
    private final String queueName;

    private final boolean increaseRetry;

    AbstractLinkPublisher(Status status, String crontab, String queueName) {
        this(status, crontab, queueName, false);
    }

    AbstractLinkPublisher(Status status, String crontab, String queueName, boolean increaseRetry) {
        this.status = status;
        this.crontab = crontab;
        this.queueName = queueName;
        this.increaseRetry = increaseRetry;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        if (Global.isTaskRunning(status.name())) {
            log.warn("%s Links Handler is already triggered and hasn't finished yet!", status);
            return;
        }

        try {
            Global.setTaskRunningStatus(status.name(), true);

            int counter = 0;
            List<Link> links = getLinks();

            while (links.size() > 0) {
                counter += links.size();

                handleLinks(links);
                setLastCheckTime(links);

                if (links.size() >= Config.DB_FETCH_LIMIT) {
                    try {
                        Thread.sleep(Config.WAITING_TIME_FOR_GETTING_LINKS_FROM_DB);
                    } catch (InterruptedException ignored) {
                    }
                    links = getLinks();
                } else {
                    links.clear();
                }
            }

            if (counter > 0)
                log.info("Task is completed. Status: %s, Number: %d", status.name(), counter);
            else
                log.info("No links in %s status found.", status.name());

        } catch (Exception e) {
            log.error("Failed to completed task!", e);
        } finally {
            Global.setTaskRunningStatus(status.name(), false);
        }

    }

    void handleLinks(List<Link> linkList) {
        for (Link link : linkList) {
            RabbitMQ.publish(queueName, link);
        }
    }

    private void setLastCheckTime(List<Link> linkList) {
        StringBuilder sb = new StringBuilder();
        for (Link link : linkList) {
            if (sb.length() > 0) sb.append(",");
            sb.append(link.getId());
        }
        Links.setLastCheckTime(sb.toString(), this.increaseRetry);
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
