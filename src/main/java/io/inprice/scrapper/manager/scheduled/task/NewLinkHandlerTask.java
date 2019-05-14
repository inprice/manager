package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.helpers.Converter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.info.LinkStatusChange;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.SiteFinder;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.io.IOException;
import java.util.List;

public class NewLinkHandlerTask extends AbstractLinkHandlerTask {

    private static final Logger log = new Logger(NewLinkHandlerTask.class);

    public NewLinkHandlerTask() {
        super(LinkStatus.NEW, Config.CRONTAB_FOR_NEW_LINKS, Config.RABBITMQ_NEW_LINKS_QUEUE);
    }

    @Override
    void handleLinks(List<Link> linksList) {
        int problemCount = 0;

        for (Link link: linksList) {
            LinkStatus newStatus = link.getStatus();
            if (SiteFinder.isValidURL(link.getUrl())) {
                Site site = SiteFinder.findSiteByUrl(link.getUrl());
                if (site != null) {
                    link.setSiteId(site.getId());
                    link.setWebsiteClassName(site.getClassName());
                } else {
                    newStatus = LinkStatus.BE_IMPLEMENTED;
                }
            } else {
                newStatus = LinkStatus.IMPROPER;
            }

            try {
                if (newStatus.equals(link.getStatus())) {
                    RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_LINK_EXCHANGE, Config.RABBITMQ_NEW_LINKS_QUEUE, null, Converter.fromObject(link));
                } else {
                    LinkStatusChange change = new LinkStatusChange(link, newStatus);
                    RabbitMQ.getChannel().basicPublish(Config.RABBITMQ_LINK_EXCHANGE, Config.RABBITMQ_CHANGE_STATUS_QUEUE, null, Converter.fromObject(change));
                }
            } catch (IOException e) {
                boolean shouldBeStopped = incProblemCount(e);
                if (shouldBeStopped) break;
            }
        }
    }

}
