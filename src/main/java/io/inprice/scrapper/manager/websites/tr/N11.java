package io.inprice.scrapper.manager.websites.tr;

import io.inprice.scrapper.common.models.LinkSpec;
import io.inprice.scrapper.manager.websites.AbstractWebsite;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class N11 extends AbstractWebsite {

    @Override
    public boolean isAvailable() {
        Element amount = doc.selectFirst("input[class='stockCount']");
        if (amount != null) {
            try {
                int realAmount = new Integer(amount.val().trim());
                return (realAmount > 0);
            } catch (Exception e) {}
        }
        return false;
    }

    @Override
    public String getCode() {
        String val = null;
        Element code = doc.selectFirst("input[class='productId']");
        if (code != null) {
            val = code.val().trim();
        }
        return val;
    }

    @Override
    public String getTitle() {
        String val = null;
        Element title = doc.selectFirst("h1.proName");
        if (title == null) title = doc.selectFirst("h1.pro-title_main");
        if (title != null) {
            val = title.text().trim();
        }
        return val;
    }

    @Override
    public BigDecimal getPrice() {
        String strPrice = null;

        Element price = doc.selectFirst(".newPrice ins");
        if (price == null) price = doc.selectFirst("ins.price-now");
        if (price != null) {
            strPrice = price.attr("content").trim();
        }

        if (strPrice == null)
            return BigDecimal.ZERO;
        else
            return new BigDecimal(cleanPrice(strPrice));
    }

    @Override
    public String getSeller() {
        String val = null;
        Element seller = doc.selectFirst("div.sallerTop h3 a");
        if (seller != null) {
            val = seller.attr("title").trim();
        } else {
            seller = doc.selectFirst(".shop-name");
            if (seller != null) {
                val = seller.text().trim();
            }
        }
        return val;
    }

    @Override
    public String getShipment() {
        String val = null;
        Element shipment = doc.selectFirst(".shipment-detail-container .cargoType");
        if (shipment == null) shipment = doc.selectFirst(".delivery-info_shipment span");

        if (shipment != null) {
            val = shipment.text().replaceAll(":", "").trim();
        }
        return val;
    }

    @Override
    public String getBrand() {
        String[] titleChunks = getTitle().split("\\s");
        if (titleChunks.length > 1) return titleChunks[0].trim();
        return null;
    }

    @Override
    public List<LinkSpec> getSpecList() {
        List<LinkSpec> specList = null;
        Elements specs = doc.select("div.feaItem");
        if (specs != null && specs.size() > 0) {
            specList = new ArrayList<>();
            for (Element spec : specs) {
                String key = spec.select(".label").text();
                String value = spec.select(".data").text();
                specList.add(new LinkSpec(key, value));
            }
        }
        return specList;
    }
}
