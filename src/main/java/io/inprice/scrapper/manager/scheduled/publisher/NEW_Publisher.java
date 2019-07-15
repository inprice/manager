package io.inprice.scrapper.manager.scheduled.publisher;

import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Site;
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
        super(Status.NEW, Config.CRONTAB_FOR_NEW_LINKS, Config.RABBITMQ_NEW_LINKS_QUEUE);
    }

    public NEW_Publisher(Status status, String crontab, String queueName) {
        super(status, crontab, queueName);
    }

    @Override
    void handleLinks(List<Link> linkList) {
        for (Link link: linkList) {
            Status problem = null;

            if (SiteFinder.isValidURL(link.getUrl())) {
                Site site = SiteFinder.findSiteByUrl(link.getUrl());
                if (site != null) {
                    link.setSiteId(site.getId());
                    link.setWebsiteClassName(site.getClassName());
                } else {
                    problem = Status.BE_IMPLEMENTED;
                }
            } else {
                problem = Status.IMPROPER;
            }

            if (problem == null) {
                RabbitMQ.publish(Config.RABBITMQ_NEW_LINKS_QUEUE, link); //the consumer class is in Worker, NewLinksConsumer
            } else {
                StatusChange change = new StatusChange(link, problem);
                RabbitMQ.publish(Config.RABBITMQ_CHANGE_EXCHANGE, Config.RABBITMQ_STATUS_CHANGE_QUEUE, change); //the consumer class is here, StatusChangeConsumer
            }
        }
    }

}
