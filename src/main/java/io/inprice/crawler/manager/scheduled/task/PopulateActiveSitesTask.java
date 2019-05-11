package io.inprice.crawler.manager.scheduled.task;

import io.inprice.crawler.common.config.Config;
import io.inprice.crawler.common.helpers.Converter;
import io.inprice.crawler.common.helpers.RabbitMQ;
import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.meta.CrawlingStatus;
import io.inprice.crawler.common.models.Link;
import io.inprice.crawler.manager.helpers.Global;
import io.inprice.crawler.manager.repository.Links;
import io.inprice.crawler.manager.scheduled.Task;
import org.quartz.*;

import java.io.IOException;
import java.util.List;

public class PopulateActiveSitesTask implements Task {

    private static final Logger log = new Logger(PopulateActiveSitesTask.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        if (Global.isAlreadyPopulatingActiveSites) {
            log.warn("PopulateActiveSitesTask is triggered but class hasn't finished previous task!");
            return;
        } else {
            log.info("PopulateActiveSitesTask is up to populate suitable sites");
        }

        int cycle = 1;
        Global.isAlreadyPopulatingActiveSites = true;
        List<Link> psList = Links.getActiveSites();

        if (psList.size() == 0) {
            log.info("There is no suitable site to be processed.");
        } else {

            int problemCount = 0;

            while (Global.isRunning && psList.size() > 0) {
                log.info("Getting active sites. Cycle: %d, Sites to be crawled: %d", cycle, psList.size());

                //joining ids
                StringBuilder idListSB = new StringBuilder("0");
                for (Link ps : psList) {
                    idListSB.append(",");
                    idListSB.append(ps.getId());
                }

                //setting crawling statuses
                boolean isUpdated = Links.setCrawlingStatuses(CrawlingStatus.PROCESSING, idListSB.toString());

                //sending them to queue
                if (isUpdated) {
                    for (Link ps : psList) {
                        try {
                            RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_EXCHANGE, Config.RABBITMQ_TASK_QUEUE, null, Converter.fromObject(ps));
                        } catch (IOException e) {
                            log.error("Error in publishing Links to RabbitMQ", e);
                            problemCount++;
                            if (problemCount >= Config.LIMIT_FOR_QUEUE_PROBLEMS) {
                                log.fatal(String.format("There should be a serious problem with RabbitMQ. Problem count has reached %d", Config.LIMIT_FOR_QUEUE_PROBLEMS));
                                log.warn("Populating sites operation is cancelled!");
                                break;
                            }
                        }
                    }
                } else {
                    log.warn("No site is sent to queue since there is a problem on db side!");
                }

                if (psList.size() == Config.DB_FETCH_LIMIT) {
                    try {
                        Thread.sleep(Config.TIME_FOR_GETTING_ACTIVE_SITES);
                    } catch (InterruptedException e) { }
                }

                if (Global.isRunning) {
                    psList = Links.getActiveSites();
                    cycle++;
                }
            }
        }
        log.info("PopulateActiveSitesTask is completed");

        Global.isAlreadyPopulatingActiveSites = false;
    }

    @Override
    public Trigger getTrigger() {
        return TriggerBuilder.newTrigger()
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInHours(4).repeatForever()
            )
        .build();
    }

    @Override
    public JobDetail getJobDetail() {
        return JobBuilder.newJob(getClass()).build();
    }
}
