package io.inprice.crawler.manager.resolver.tr;

import io.inprice.crawler.common.models.LinkSpec;
import io.inprice.crawler.manager.resolver.AbstractResolver;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GittiGidiyorResolver extends AbstractResolver {

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
    public String getCode() {
        return null;
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
