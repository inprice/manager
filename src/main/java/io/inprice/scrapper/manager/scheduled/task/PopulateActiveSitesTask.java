package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.io.IOException;
import java.util.List;

public class PopulateActiveSitesTask implements Task {

    private static final Logger log = new Logger(PopulateActiveSitesTask.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        if (Global.isAlreadyPopulatingActiveSites) {
            log.warn("PopulateActiveSitesTask is triggered but a previous task hasn't finished yet!");
            return;
        } else {
            log.info("PopulateActiveSitesTask is up to populate suitable sites");
        }

        synchronized (log) {
            Global.isAlreadyPopulatingActiveSites = true;
            Global.cycle++;
        }

        List<Link> links = Links.getActiveSites();
        if (links.size() == 0) {
            log.info("There is no suitable site to be processed.");
        } else {

            int problemCount = 0;
            while (Global.isRunning && links.size() > 0) {
                log.info("Getting active sites. Cycle: %d, Sites to be scrapped: %d", Global.cycle, links.size());

                //joining ids
                StringBuilder idListSB = new StringBuilder("0");
                for (Link ps : links) {
                    idListSB.append(",");
                    idListSB.append(ps.getId());
                }

                boolean isUpdated = Links.updateCycleValues(idListSB.toString());

                //sending them to queue
                if (isUpdated) {
                    for (Link link: links) {
                        try {
                            RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_EXCHANGE, Config.RABBITMQ_TASK_QUEUE, null, Converter.fromObject(link));
                        } catch (IOException e) {
                            log.error("Failed to send Links to RabbitMQ", e);
                            problemCount++;
                            if (problemCount >= Config.LIMIT_FOR_QUEUE_PROBLEMS) {
                                log.fatal(String.format("There should be a serious problem with RabbitMQ. Problem count has reached %d", Config.LIMIT_FOR_QUEUE_PROBLEMS));
                                log.warn("Populating sites operation in this cycle cancelled!");
                                break;
                            }
                        }
                    }
                } else {
                    log.warn("There is a problem on db side, so no site will be sent to queue!");
                    log.warn("Populating sites operation in this cycle cancelled!");
                    break;
                }

                if (links.size() == Config.DB_FETCH_LIMIT) {
                    try {
                        Thread.sleep(Config.TIME_FOR_GETTING_ACTIVE_SITES);
                    } catch (InterruptedException e) { }
                }

                if (Global.isRunning) {
                    links = Links.getActiveSites();
                }
            }
        }
        log.info("PopulateActiveSitesTask is completed.");

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
