package io.inprice.scrapper.manager.websites;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.manager.helpers.SiteFinder;

public class WebsiteResolver {

    private static final Logger log = new Logger(WebsiteResolver.class);

    public static Link resolve(Link link) {
        Site site = SiteFinder.findSiteByUrl(link.getUrl());
        if (site == null) {
            link.setStatus(LinkStatus.UNKNOWN);
            return link;
        } else {
            link.setSiteId(site.getId());
        }

        try {
            Class<Website> resolverClass = (Class<Website>) Class.forName(site.getClassName());
            Website website = resolverClass.newInstance();
            website.check(link);
        } catch (Exception e) {
            link.setStatus(LinkStatus.IMPROPER);
            log.warn("%s : URL Problem [%s] - %s", site.getName(), link.getStatus().name(), link.getUrl());
            log.error(e);
        }

        return link;
    }

}
