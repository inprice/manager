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
        Global.setTaskRunningStatus(status.name(), true);

        try {
            List<Link> links = getLinks();
            if (links.size() > 0) {
                //chop list into DB_FETCH_LIMIT and handle them smaller blocks
                if (links.size() > Config.DB_FETCH_LIMIT) {
                    int start = 0;
                    int stop = Config.DB_FETCH_LIMIT;

                    while (start < links.size()) {
                        //TODO: this operation may be executed in an executor pool!!!
                        List<Link> sublist = links.subList(start, stop);
                        handleLinks(sublist);
                        setLastCheckTime(links);
                        try {
                            Thread.sleep(Config.WAITING_TIME_FOR_GETTING_LINKS_FROM_DB);
                        } catch (InterruptedException e) {
                            //
                        }
                        start = stop;
                        stop += Config.DB_FETCH_LIMIT;
                    }

                } else {
                    handleLinks(links);
                    setLastCheckTime(links);
                }
                log.info("%d of %s link completed.", links.size(), status.name());
            }
        } catch (Exception e) {
            log.error("Failed to completed job!", e);
        }

        Global.setTaskRunningStatus(status.name(), false);
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
