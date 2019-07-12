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
 * Finds links in NEW and RENEWED status and sends them to collect data in Worker project
 *
 * @author mdpinar
 */
public class NewLinksPublisher extends AbstractLinkPublisher {

    public NewLinksPublisher(Status status, String crontab) {
        super(status, crontab, Config.RABBITMQ_NEW_LINKS_QUEUE);
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
                RabbitMQ.publish(Config.RABBITMQ_STATUS_CHANGE_QUEUE, change); //the consumer class is here, StatusChangeConsumer
            }
        }
    }

}
