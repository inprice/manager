package io.inprice.scrapper.manager.scheduled.publishers;

import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.io.IOException;
import java.util.List;

/**
 * NEW LINKS, ACTIVE LINKS and the links which have SOCKET_ERROR are handled by this publishers
 */
public abstract class AbstractLinkPublisher implements Task {

    private static final Logger log = new Logger("LinkHandlerTask");

    private final Status status;
    private final String crontab;
    private final String queueName;

    private boolean willRetryBeIncremented;
    private int retryLimit;
    private int problemCount = 0;

    private volatile boolean isRunning = false;

    AbstractLinkPublisher(Status status, String crontab, String queueName) {
        this.status = status;
        this.crontab = crontab;
        this.queueName = queueName;
    }

    AbstractLinkPublisher(Status status, String crontab, String queueName, boolean willRetryBeIncremented, int retryLimit) {
        this.status = status;
        this.crontab = crontab;
        this.queueName = queueName;
        this.willRetryBeIncremented = willRetryBeIncremented;
        this.retryLimit = retryLimit;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        if (isRunning) {
            log.warn("%s Link Handler is triggered but a previous publishers hasn't finished yet!", status);
            return;
        }

        int cycle = 0;

        synchronized (log) {
            cycle = incCycle();
            isRunning = true;
            problemCount = 0;
        }

        log.info("%s link handler is starting for cycle %d...", status, cycle);

        List<Link> links = getLinks();
        if (links.size() == 0) {
            log.info("There is no suitable %s link.", status);
        } else {

            while (Global.isApplicationRunning && links.size() > 0) {
                log.info("%d %s links will be evaluated in cycle %d", links.size(), status, cycle);

                //joining ids
                StringBuilder idListSB = new StringBuilder("0");
                for (Link ps : links) {
                    idListSB.append(",");
                    idListSB.append(ps.getId());
                }

                boolean isUpdated = updateLinks(idListSB.toString());

                if (isUpdated) {
                    handleLinks(links);
                } else {
                    log.warn("There is a problem on db side, so no link will be sent to queue!");
                    log.warn("%s link handling operation in cycle %d cancelled!", status, cycle);
                    break;
                }

                if (links.size() == Config.DB_FETCH_LIMIT) {
                    try {
                        Thread.sleep(Config.WAITING_TIME_FOR_GETTING_LINKS_FROM_DB);
                    } catch (InterruptedException e) {
                        //nothing to do
                    }
                }

                if (Global.isApplicationRunning) links = getLinks();
            }
        }
        log.info("%s Link Handler is completed cycle %d.", status, cycle);

        isRunning = false;
    }

    List<Link> getLinks() {
        return Links.getLinks(getStatus(), getCycle());
    }

    void handleLinks(List<Link> linksList) {
        for (Link link: linksList) {
            try {
                RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_LINK_EXCHANGE, getQueueName(), null, Converter.fromObject(link));
            } catch (IOException e) {
                boolean shouldBeStopped = incProblemCount(e);
                if (shouldBeStopped) break;
            }
        }
    }

    private boolean updateLinks(String idList) {
        return Links.updateCycleValues(status, getCycle(), idList, willRetryBeIncremented);
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

    boolean incProblemCount(Throwable t) {
        problemCount++;
        if (problemCount >= Config.RETRY_LIMIT_FOR_QUEUE_PROBLEMS) {
            log.fatal(String.format("There should be a serious problem with RabbitMQ. Problem count has reached %d", Config.RETRY_LIMIT_FOR_QUEUE_PROBLEMS));
            log.warn("%s link handling operation in cycle %d cancelled!", getStatus(), getCycle());
            return true;
        }
        log.error(t);

        return false;
    }

    Status getStatus() {
        return status;
    }

    String getQueueName() {
        return queueName;
    }

    int getRetryLimit() {
        return retryLimit;
    }

    private int incCycle() {
        Integer val = Global.statusCycleMap.get(getStatus());
        if (val == null) val = 0;
        ++val;
        Global.statusCycleMap.put(getStatus(), val);
        return val;
    }

    int getCycle() {
        return Global.statusCycleMap.get(getStatus());
    }
}
