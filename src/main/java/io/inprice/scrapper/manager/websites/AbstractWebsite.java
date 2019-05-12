package io.inprice.scrapper.manager.websites;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.SiteFinder;
import io.inprice.scrapper.manager.helpers.UserAgents;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.math.BigDecimal;

public abstract class AbstractWebsite implements Website {

    private static final Logger log = new Logger(AbstractWebsite.class);

    protected Document doc;

    @Override
    public void check(Link link) {
        createDoc(link);

        if (LinkStatus.NEW.equals(link.getStatus()) || LinkStatus.ACTIVE.equals(link.getStatus())) {
            if (LinkStatus.NEW.equals(link.getStatus())) {
                link.setStatus(LinkStatus.ACTIVE);
                link.setTitle(getTitle());
                link.setCode(getCode());
                link.setSeller(getSeller());
                link.setShipment(getShipment());
                link.setBrand(getBrand());
                link.setSpecList(getSpecList());
            }

            if (isAvailable()) {
                BigDecimal price = getPrice();
                if (!price.equals(link.getPrice())) {
                    sendAPriceChangeMessage(link, price);
                    link.setPrice(getPrice());
                }

                log.debug("Price : %f, Seller: %s, Shipment: %s, Brand: %s", link.getPrice(), link.getSeller(), link.getShipment(), link.getBrand());
            } else {
                link.setStatus(LinkStatus.UNAVAILABLE);
                log.debug("This product is now unavailable!");
            }

        } else {
            sendAStatusChangeMessage(link);
            log.warn("URL Problem [%s] - %s", link.getStatus().name(), link.getUrl());
        }
    }

    private void sendAPriceChangeMessage(Link link, BigDecimal newPrice) {

    }

    private void sendAStatusChangeMessage(Link link) {

    }

    protected String cleanPrice(String price) {
        return price.replace(",", ".").replaceAll("[^\\d.]", "").trim();
    }

    private void createDoc(Link link) {
        try {
            if (SiteFinder.isValidURL(link.getUrl())) {
                doc = getDocument(link.getUrl());
            } else {
                link.setStatus(LinkStatus.IMPROPER);
                log.warn("Wrong url: %s", link.getUrl());
            }
        } catch (IOException e) {
            link.setStatus(LinkStatus.RESETTED);
            log.error("Failed to parse url: %s", link.getUrl());
        }
    }

    private Document getDocument(String url) throws IOException {
        Connection.Response res = Jsoup.connect(url)
                .userAgent(UserAgents.findARandomUA())
                .referrer(UserAgents.findARandomReferer())
                .timeout(5000)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .execute();
        log.info("Status Code: %d", res.statusCode());
        return res.parse();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
