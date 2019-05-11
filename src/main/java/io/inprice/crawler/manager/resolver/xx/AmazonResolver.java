package io.inprice.crawler.manager.resolver.xx;

import io.inprice.crawler.common.models.LinkSpec;
import io.inprice.crawler.manager.resolver.AbstractResolver;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * asin den buldurma https://www.amazon.co.uk/dp/B00VX62MHO
 */
public class AmazonResolver extends AbstractResolver {

    @Override
    public String getTitle() {
        String val = null;
        Element title = doc.getElementById("productTitle");
        if (title != null) {
            val = title.text().trim();
        }
        return val;
    }

    @Override
    public String getCode() {
        String val = null;
        Element code = doc.getElementById("ASIN");
        if (code != null) {
            val = code.val().trim();
        }
        return val;
    }

    @Override
    public BigDecimal getPrice() {
        String strPrice = null;

        Element price = doc.select("#cerberus-data-metrics").first();
        if (price != null) strPrice = price.attr("data-asin-price");

        if (strPrice == null || strPrice.isEmpty()) {
            price = doc.select(".header-price").first();
            if (price == null) price = doc.select("#olp-new .a-color-price").first();
            if (price == null) price = doc.select("#priceblock_dealprice").first();
            if (price == null) price = doc.select(".a-size-medium.a-color-price.offer-price.a-text-normal").first();

            if (price != null) {
                strPrice = price.text();
            } else {
                price = doc.select(".price-large").first();
                if (price != null) {
                    String left = cleanPrice(price.text());
                    String right = "00";
                    if (price.nextElementSibling() != null) {
                        right = price.nextElementSibling().text();
                    }
                    strPrice = left + "." + right;
                } else {
                    price = doc.select("#priceblock_ourprice").first();
                    if (price != null) {
                        if (price.text().contains("-")) {
                            String[] priceChunks = price.text().split("-");
                            String first = cleanPrice(priceChunks[0]);
                            String second = cleanPrice(priceChunks[1]);
                            BigDecimal low = new BigDecimal(first);
                            BigDecimal high = new BigDecimal(second);
                            strPrice = high.add(low).divide(BigDecimal.valueOf(2)).toString();
                        } else {
                            strPrice = price.text();
                        }
                    }
                }
            }
        }

        if (strPrice == null)
            return BigDecimal.ZERO;
        else
            return new BigDecimal(cleanPrice(strPrice));
    }

    @Override
    public String getSeller() {
        String val = null;
        Element seller = doc.getElementById("bylineInfo");
        if (seller != null) {
            val = seller.text().trim();
        } else {
            val = "amazon";
        }
        return val;
    }

    @Override
    public String getShipment() {
        String val = null;
        Element shipment = doc.select("#price-shipping-message").first();
        if (shipment == null) shipment = doc.select("#ddmDeliveryMessage span").first();
        if (shipment == null) shipment = doc.select(".shipping3P").first();

        if (shipment != null) {
            val = shipment.text().trim();
        }
        return val;
    }

    @Override
    public String getBrand() {
        return null;
    }

    @Override
    public List<LinkSpec> getSpecList() {
        List<LinkSpec> specList = null;
        Elements specs = doc.select("#feature-bullets li:not(.aok-hidden)");
        if (specs != null && specs.size() > 0) {
            specList = new ArrayList<>();
            for (int i = 0; i < specs.size(); i++) {
                specList.add(new LinkSpec("", specs.get(i).text().trim()));
            }
        }
        return specList;
    }
}
