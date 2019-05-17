package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.io.IOException;
import java.util.List;

/**
 * NEW LINKS, ACTIVE LINKS and the links which have SOCKET_ERROR are handled by this task
 */
public abstract class AbstractLinkHandlerTask implements Task {

    private static final Logger log = new Logger("LinkHandlerTask");

    private final LinkStatus linkStatus;
    private final String crontab;
    private final String queueName;

    private boolean willRetryBeIncremented;
    private int retryLimit;
    private int problemCount = 0;

    private volatile boolean isRunning = false;

    public AbstractLinkHandlerTask(LinkStatus status, String crontab, String queueName) {
        this.linkStatus = status;
        this.crontab = crontab;
        this.queueName = queueName;
    }

    public AbstractLinkHandlerTask(LinkStatus linkStatus, String crontab, String queueName, boolean willRetryBeIncremented, int retryLimit) {
        this.linkStatus = linkStatus;
        this.crontab = crontab;
        this.queueName = queueName;
        this.willRetryBeIncremented = willRetryBeIncremented;
        this.retryLimit = retryLimit;
    }

    abstract void handleLinks(List<Link> linksList);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        if (isRunning) {
            log.warn("%s Link Handler is triggered but a previous task hasn't finished yet!", linkStatus);
            return;
        }

        int cycle = 0;

        synchronized (log) {
            cycle = incCycle();
            isRunning = true;
            problemCount = 0;
        }

        log.info("%s link handler is starting for cycle %d...", linkStatus, cycle);

        List<Link> links = getLinks();
        if (links.size() == 0) {
            log.info("There is no suitable %s link.", linkStatus);
        } else {

            while (Global.isApplicationRunning && links.size() > 0) {
                log.info("%d %s links will be evaluated in cycle %d", links.size(), linkStatus, cycle);

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
                    log.warn("%s link handling operation in cycle %d cancelled!", linkStatus, cycle);
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
        log.info("%s Link Handler is completed cycle %d.", linkStatus, cycle);

        isRunning = false;
    }

    List<Link> getLinks() {
        return Links.getLinks(getLinkStatus(), getCycle());
    }

    private boolean updateLinks(String idList) {
        return Links.updateCycleValues(linkStatus, getCycle(), idList, willRetryBeIncremented());
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
            log.warn("%s link handling operation in cycle %d cancelled!", getLinkStatus(), getCycle());
            return true;
        }

        return false;
    }

    LinkStatus getLinkStatus() {
        return linkStatus;
    }

    String getQueueName() {
        return queueName;
    }

    int getRetryLimit() {
        return retryLimit;
    }

    boolean willRetryBeIncremented() {
        return willRetryBeIncremented;
    }

    private int incCycle() {
        Integer val = Global.linkStatusCycleMap.get(getLinkStatus());
        if (val == null) val = 0;
        ++val;
        Global.linkStatusCycleMap.put(getLinkStatus(), val);
        return val;
    }

    int getCycle() {
        return Global.linkStatusCycleMap.get(getLinkStatus());
    }
}
