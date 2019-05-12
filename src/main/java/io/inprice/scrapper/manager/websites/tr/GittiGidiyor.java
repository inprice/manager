package io.inprice.scrapper.manager.websites.tr;

import io.inprice.scrapper.common.models.LinkSpec;
import io.inprice.scrapper.manager.websites.AbstractWebsite;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GittiGidiyor extends AbstractWebsite {

    @Override
    public boolean isAvailable() {
        Element amount = doc.getElementById("VariantProductRemaingCount");
        if (amount != null) {
            try {
                int realAmount = new Integer(amount.text().trim());
                return (realAmount > 0);
            } catch (Exception e) {}
        }
        return false;
    }

    @Override
    public String getCode() {
        String val = null;
        Element code = doc.getElementById("productId");
        if (code != null) {
            val = code.val().trim();
        }
        return val;
    }

    @Override
    public String getTitle() {
        String val = null;
        Element title = doc.getElementById("productTitle");
        if (title == null) title = doc.selectFirst("span.title");
        if (title != null) {
            val = title.text().trim();
        }
        return val;
    }

    @Override
    public BigDecimal getPrice() {
        String strPrice = null;

        Element price = doc.selectFirst("[data-price]");
        if (price != null) {
            strPrice = price.attr("data-price").trim();
        }

        if (strPrice == null)
            return BigDecimal.ZERO;
        else
            return new BigDecimal(cleanPrice(strPrice));
    }

    @Override
    public String getSeller() {
        String val = null;
        Element seller = doc.selectFirst(".member-name a strong");
        if (seller != null) {
            val = seller.text().trim();
        }
        return val;
    }

    @Override
    public String getShipment() {
        String val = null;
        Element shipment = doc.selectFirst(".CargoInfos .color-black");
        if (shipment != null) {
            val = shipment.text().trim();
        }
        return val;
    }

    @Override
    public String getBrand() {
        String val = null;
        Element brand = doc.selectFirst(".mr10.gt-product-brand-0 a");
        if (brand != null) {
            val = brand.text().trim();
        }
        return val;
    }

    @Override
    public List<LinkSpec> getSpecList() {
        List<LinkSpec> specList = null;
        Elements specs = doc.select("#specs-container ul li");
        if (specs != null && specs.size() > 0) {
            specList = new ArrayList<>();
            for (Element spec : specs) {
                String key = spec.select("span").text();
                String value = spec.select("strong").text().replaceAll(":", "").trim();
                specList.add(new LinkSpec(key, value));
            }
        }
        return specList;
    }

}
