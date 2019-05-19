package io.inprice.scrapper.manager.scheduled.publishers;

import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.RabbitMQ;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.manager.helpers.SiteFinder;
import io.inprice.scrapper.manager.repository.Links;

import java.io.IOException;
import java.util.List;

public class NewLinkPublisher extends AbstractLinkPublisher {

    public NewLinkPublisher() {
        super(Status.NEW, Config.CRONTAB_FOR_NEW_LINKS, Config.RABBITMQ_NEW_LINKS_QUEUE);
    }

    @Override
    void handleLinks(List<Link> linkList) {
        for (Link link: linkList) {
            Status newStatus = link.getStatus();
            if (SiteFinder.isValidURL(link.getUrl())) {
                Site site = SiteFinder.findSiteByUrl(link.getUrl());
                if (site != null) {
                    link.setSiteId(site.getId());
                    link.setWebsiteClassName(site.getClassName());
                    boolean isOK = Links.setSiteAndWebsiteClassName(link.getId(), site.getId(), site.getClassName());
                    if (! isOK) {
                        newStatus = Status.INTERNAL_ERROR;
                    }
                } else {
                    newStatus = Status.BE_IMPLEMENTED;
                }
            } else {
                newStatus = Status.IMPROPER;
            }

            try {
                if (newStatus.equals(link.getStatus())) {
                    RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_LINK_EXCHANGE, Config.RABBITMQ_NEW_LINKS_QUEUE, null, Converter.fromObject(link));
                } else {
                    StatusChange change = new StatusChange(link, newStatus);
                    RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_LINK_EXCHANGE, Config.RABBITMQ_STATUS_CHANGE_QUEUE, null, Converter.fromObject(change));
                }
            } catch (IOException e) {
                boolean shouldBeStopped = incProblemCount(e);
                if (shouldBeStopped) break;
            }
        }
    }

}
