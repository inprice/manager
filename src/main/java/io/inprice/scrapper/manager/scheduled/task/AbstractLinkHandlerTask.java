package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.util.List;

/**
 * NEW LINKS, ACTIVE LINKS and the links which have SOCKET_ERROR are handled by this task
 */
public abstract class AbstractLinkHandlerTask implements Task {

    private static final Logger log = new Logger(AbstractLinkHandlerTask.class);

    private final LinkStatus linkStatus;
    private final String crontab;
    private final String queueName;
    private int problemCount = 0;

    private volatile int cycle = 0;
    private volatile boolean isRunning = false;

    public AbstractLinkHandlerTask(LinkStatus status, String crontab, String queueName) {
        this.linkStatus = status;
        this.crontab = crontab;
        this.queueName = queueName;
    }

    abstract void handleLinks(List<Link> linksList);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        if (isRunning) {
            log.warn("%s Link Handler is triggered but a previous task hasn't finished yet!", linkStatus);
            return;
        }

        synchronized (log) {
            cycle++;
            isRunning = true;
            problemCount = 0;
        }

        log.info("%s link handler is starting for %d cycle...", linkStatus, cycle);

        List<Link> links = Links.getLinks(linkStatus, cycle);
        if (links.size() == 0) {
            log.info("There is no suitable %s link.", linkStatus);
        } else {

            while (Global.isRunning && links.size() > 0) {
                log.info("%d %s links will be evaluated in cycle %d", links.size(), linkStatus, cycle);

                //joining ids
                StringBuilder idListSB = new StringBuilder("0");
                for (Link ps : links) {
                    idListSB.append(",");
                    idListSB.append(ps.getId());
                }

                boolean isUpdated = Links.updateCycleValues(linkStatus, cycle, idListSB.toString());

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

                if (Global.isRunning) {
                    links = Links.getLinks(linkStatus, cycle);
                }
            }
        }
        log.info("%s Link Handler is completed cycle %d.", linkStatus, cycle);

        isRunning = false;
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

    int getCycle() {
        return cycle;
    }

    String getCrontab() {
        return crontab;
    }

    String getQueueName() {
        return queueName;
    }
}
