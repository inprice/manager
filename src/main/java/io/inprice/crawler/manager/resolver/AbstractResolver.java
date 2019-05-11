package io.inprice.crawler.manager.resolver;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.meta.LinkStatus;
import io.inprice.crawler.common.models.Link;
import io.inprice.crawler.common.models.LinkHistory;
import io.inprice.crawler.common.models.LinkPrice;
import io.inprice.crawler.manager.helpers.SiteFinder;
import io.inprice.crawler.manager.helpers.UserAgents;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class AbstractResolver implements Resolver {

    private static final Logger log = new Logger(AbstractResolver.class);

    private Link link = new Link();
    protected Document doc;

    @Override
    public Link resolve(String url, boolean print) {
        createDoc(url);

        if (! link.getStatus().equals(LinkStatus.IMPROPER)) {
            link.setUrl(url);
            link.setStatus(LinkStatus.ACTIVE);

            link.setTitle(getTitle());
            link.setCode(getCode());
            link.setPrice(getPrice());
            link.setSeller(getSeller());
            link.setShipment(getShipment());
            link.setBrand(getBrand());
            link.setSpecList(getSpecList());

            //history list
            List<LinkHistory> historyList = new ArrayList<>();
            historyList.add(new LinkHistory(link.getStatus()));
            link.setHistoryList(historyList);

            //price date and list
            if (link.getPrice() != null) {
                link.setPriceDate(new Date());

                List<LinkPrice> priceList = new ArrayList<>();
                priceList.add(new LinkPrice(link.getPrice(), ""));
                link.setPriceList(priceList);
            }

            log.debug("Price : %f, Seller: %s, Shipment: %s, Brand: %s", link.getPrice(), link.getSeller(), link.getShipment(), link.getBrand());
        }

        if (print) printOut();

        return link;
    }

    private Document getDocument(String url) throws IOException {
        Connection.Response res = Jsoup.connect(url)
                .userAgent(UserAgents.findARandomUA())
                .referrer(UserAgents.findARandomReferer())
                .timeout(12000)
                .ignoreContentType(true)
                .followRedirects(true)
                .execute();
        return res.parse();
    }

    protected String cleanPrice(String price) {
        return price.replace(",", ".").replaceAll("[^\\d.]", "").trim();
    }

    private void createDoc(String url) {
        try {
            if (SiteFinder.isValidURL(url)) {
                this.doc = getDocument(url);
                link.setStatus(LinkStatus.ACTIVE);
            } else {
                log.warn("Wrong url: %s", url);
                link.setStatus(LinkStatus.IMPROPER);
            }
        } catch (IOException e) {
            log.error("Error in parsing url: %s", url, e);
            link.setStatus(LinkStatus.IMPROPER);
        }
    }

    private void printOut() {
        log.debug("--------------------------------------------------------------------------------------------------");
        log.debug("Code  : " + link.getCode());
        log.debug("Title : " + link.getTitle());
        log.debug("Price : %f, Seller: %s, Shipment: %s, Brand: %s", link.getPrice(), link.getSeller(), link.getShipment(), link.getBrand());
        if (link.getSpecList() != null && link.getSpecList().size() > 0) {
            link.getSpecList().forEach(spec -> {
                log.debug("  > " + spec.getKey() + " - " + spec.getValue());
            });
        }
    }
}
