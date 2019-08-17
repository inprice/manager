package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.common.utils.URLUtils;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.manager.helpers.SiteFinder;

import java.util.List;

/**
 * Finds and handles NEW (and also RENEWED through inheritance) links
 *
 * @author mdpinar
 */
public class NEW_Publisher extends AbstractLinkPublisher {

    public NEW_Publisher() {
        super(Status.NEW, Config.CRON_FOR_NEW_LINKS, Config.MQ_NEW_LINKS_QUEUE);
    }

    NEW_Publisher(Status status, String crontab, String queueName) {
        super(status, crontab, queueName);
    }

    @Override
    void handleLinks(List<Link> linkList) {
        for (Link link: linkList) {
            Status oldStatus = link.getStatus();

            if (URLUtils.isAValidURL(link.getUrl())) {
                Site site = SiteFinder.findSiteByUrl(link.getUrl());
                if (site != null) {
                    link.setSiteId(site.getId());
                    link.setWebsiteClassName(site.getClassName());
                } else {
                    link.setStatus(Status.BE_IMPLEMENTED);
                }
            } else {
                link.setStatus(Status.IMPROPER);
            }

            if (link.getStatus().equals(oldStatus)) {
                RabbitMQ.publish(Config.MQ_NEW_LINKS_QUEUE, link); //the consumer class is in Worker, NewLinksConsumer
            } else {
                StatusChange change = new StatusChange(link, oldStatus);
                RabbitMQ.publish(Config.MQ_CHANGE_EXCHANGE, Config.MQ_STATUS_CHANGE_QUEUE, change); //the consumer class is here, StatusChangeConsumer
            }
        }
    }

}
