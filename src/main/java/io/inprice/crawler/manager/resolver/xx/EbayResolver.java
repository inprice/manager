package io.inprice.crawler.manager.resolver.xx;

import io.inprice.crawler.common.models.LinkSpec;
import io.inprice.crawler.manager.resolver.AbstractResolver;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * itemid den buldurma : https://www.ebay.de/itm/372661939240
 */
public class EbayResolver extends AbstractResolver {

    @Override
    public String getTitle() {
        String val = null;
        Element title = doc.selectFirst("a[data-itemid]");
        if (title != null) {
            val = title.attr("etafsharetitle").trim();
        }
        return val;
    }

    @Override
    public String getCode() {
        String val = null;
        Element code = doc.selectFirst("a[data-itemid]");
        if (code != null) {
            val = code.attr("data-itemid").trim();
        }
        return val;
    }

    @Override
    public BigDecimal getPrice() {
        String strPrice = null;

        Element price = doc.select("#prcIsum").first();
        if (price == null) price = doc.select("#prcIsum_bidPrice").first();

        if (price != null) {
            strPrice = price.attr("content").trim();
        } else {
            price = doc.select("#mm-saleDscPrc").first();
            if (price != null) {
                strPrice = price.text().replaceAll("[^\\d.]", "").trim();
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
        Element seller = doc.select("#mbgLink").first();
        if (seller != null) {
            String[] sellerChunks = seller.attr("aria-label").split(":");
            if (sellerChunks.length > 1) {
                val = sellerChunks[1].trim();
            }
        }
        return val;
    }

    @Override
    public String getShipment() {
        String val = null;
        Element shipment = doc.select("#fshippingCost span").first();
        if (shipment == null) shipment = doc.select("#shSummary span").first();

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
        Elements specs = doc.select("table[role='presentation']:not(#itmSellerDesc) td");
        if (specs != null && specs.size() > 0) {
            specList = new ArrayList<>();
            for (int i = 0; i < specs.size(); i++) {
                String key = specs.get(i).text().replaceAll(":", "").trim();
                String value = "";
                if (i < specs.size()-1) {
                    value = specs.get(++i).text().trim();
                }
                specList.add(new LinkSpec(key, value));
            }
        }
        return specList;
    }
}
