package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.utils.DateUtils;
import io.inprice.scrapper.manager.config.Properties;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.repository.LinkRepository;
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
    static final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);
    static final Properties props = Beans.getSingleton(Properties.class);

    /*
     * Same classes and logic can be used for both links and imported product definition rows
     */
    private boolean lookForImportedProducts;

    abstract Status getStatus();
    abstract String getTimePeriodStatement();
    abstract String getMQRoutingKey();

    AbstractLinkPublisher() {
        this.lookForImportedProducts = false;
    }

    AbstractLinkPublisher(boolean lookForImportedProducts) {
        this.lookForImportedProducts = lookForImportedProducts;
    }

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
                        Thread.sleep(props.getWT_ForGettingLinksFromDB());
                    } catch (InterruptedException ignored) {
                    }
                    links = getLinks();
                } else {
                    links.clear();
                }
            }

            if (counter > 0)
                log.info("[{} -> Imported: {}] link(s) is handled successfully. Number: {}, Time: {}", getStatus().name(), lookForImportedProducts, counter, (System.currentTimeMillis() - startTime));
            else
                log.info("No link [{} -> Imported: {}] status found.", getStatus().name(), lookForImportedProducts);

        } catch (Exception e) {
            log.error(String.format("Failed to completed %s task!", getStatus().name()), e);
        } finally {
            Global.setTaskRunningStatus(getStatus().name(), false);
        }

    }

    @Override
    public TimePeriod getTimePeriod() {
        TimePeriod tp = DateUtils.parseTimePeriod(this.getTimePeriodStatement());

        //if looking for imported products then interval is set fifteen per cent more of normal value
        if (lookForImportedProducts) {
            tp = new TimePeriod((int)(tp.getInterval() * 1.15), tp.getTimeUnit());
        }
        return tp;
    }

    void handleLinks(List<Link> linkList) {
        for (Link link : linkList) {
            RabbitMQ.publish(getMQRoutingKey(), link);
        }
    }

    List<Link> getLinks() {
        return linkRepository.getLinks(getStatus(), lookForImportedProducts);
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
        linkRepository.setLastCheckTime(sb.toString(), isIncreaseRetry());
    }

    boolean isLookingForImportedProducts() {
        return lookForImportedProducts;
    }

}
