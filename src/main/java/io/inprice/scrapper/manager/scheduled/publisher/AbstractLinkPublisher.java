package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.utils.DateUtils;
import io.inprice.scrapper.manager.config.Properties;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.scheduled.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This is a class containing common functions used by all the publishers.
 * Since quartz needs to create publisher classes through their default (without args) constructors,
 * in each publisher, please add a default constructor referencing this class's constructors.
 *
 * @author mdpinar
 */
public abstract class AbstractLinkPublisher implements Task {

    private static final Logger log = LoggerFactory.getLogger("LinkHandlerTask");
    static final Properties props = Beans.getSingleton(Properties.class);

    abstract Status getStatus();
    abstract String getTimePeriodStatement();
    abstract String getMQRoutingKey();

    @Override
    public void run() {
        if (Global.isTaskRunning(getStatus().name())) {
            log.warn("{} link handler is already triggered and hasn't finished yet!", getStatus());
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            Global.setTaskRunningStatus(getStatus().name(), true);

            int counter = 0;
            List<Link> links = getLinks();

            while (links.size() > 0) {
                counter += links.size();

                handleLinks(links);
                setLastCheckTime(links);

                if (links.size() >= props.getDB_FetchLimit()) {
                    try {
                        Thread.sleep(props.getWTF_GettingLinksFromDB());
                    } catch (InterruptedException ignored) {
                    }
                    links = getLinks();
                } else {
                    links.clear();
                }
            }

            if (counter > 0)
                log.info("{} link(s) is handled successfully. Number: {}, Time: {}", getStatus().name(), counter, (System.currentTimeMillis() - startTime));
            else
                log.info("No link in {} status found.", getStatus().name());

        } catch (Exception e) {
            log.error(String.format("Failed to completed %s task!", getStatus().name()), e);
        } finally {
            Global.setTaskRunningStatus(getStatus().name(), false);
        }

    }

    @Override
    public TimePeriod getTimePeriod() {
        return DateUtils.parseTimePeriod(this.getTimePeriodStatement());
    }

    void handleLinks(List<Link> linkList) {
        for (Link link : linkList) {
            RabbitMQ.publish(getMQRoutingKey(), link);
        }
    }

    List<Link> getLinks() {
        return Links.getLinks(getStatus());
    }

    boolean isIncreaseRetry() {
        return false;
    }

    private void setLastCheckTime(List<Link> linkList) {
        StringBuilder sb = new StringBuilder();
        for (Link link : linkList) {
            if (sb.length() > 0) sb.append(",");
            sb.append(link.getId());
        }
        Links.setLastCheckTime(sb.toString(), isIncreaseRetry());
    }
}
