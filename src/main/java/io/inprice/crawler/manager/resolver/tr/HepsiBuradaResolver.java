package io.inprice.crawler.manager.resolver.tr;

import io.inprice.crawler.common.models.LinkSpec;
import io.inprice.crawler.manager.resolver.AbstractResolver;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class HepsiBuradaResolver extends AbstractResolver {

    @Override
    public String getTitle() {
        String val = null;
        Element title = doc.getElementById("product-name");
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

        Element price = doc.getElementById("offering-price");
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
        Element seller = doc.selectFirst(".brand-name a");
        if (seller != null) {
            val = seller.text().trim();
        }
        return val;
    }

    @Override
    public String getShipment() {
        return null;
    }

    @Override
    public String getBrand() {
        String val = null;
        Element brand = doc.selectFirst(".brand-name a");
        if (brand != null) {
            val = brand.text().trim();
        }
        return val;
    }

    @Override
    public List<LinkSpec> getSpecList() {
        List<LinkSpec> specList = null;
        Elements specs = doc.select(".data-list.tech-spec tr");
        if (specs != null && specs.size() > 0) {
            specList = new ArrayList<>();
            for (Element spec : specs) {
                String key = spec.select("th").text();
                String value = spec.select("td").text();
                specList.add(new LinkSpec(key, value));
            }
        }
        return specList;
    }
}
