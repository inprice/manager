package io.inprice.crawler.manager.resolver;

import io.inprice.crawler.common.meta.LinkStatus;
import io.inprice.crawler.common.models.Link;
import io.inprice.crawler.common.models.Site;
import io.inprice.crawler.manager.helpers.SiteFinder;

public class LinkResolver {

    private String url;

    public LinkResolver(String url) {
        this.url = url;
    }

    public Link resolve() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Link link = new Link();
        link.setUrl(url);
        link.setStatus(LinkStatus.NOT_FOUND);

        Site site = SiteFinder.findSiteByUrl(this.url);
        if (site == null) return link;

        Class<Resolver> resolverClass =
            (Class<Resolver>) Class.forName(String.format("io.inprice.crawler.manager.resolver.%sResolver", site.getClassName()));

        Resolver resolver = resolverClass.newInstance();
        resolver.resolve(url, true);

        return link;
    }

}
