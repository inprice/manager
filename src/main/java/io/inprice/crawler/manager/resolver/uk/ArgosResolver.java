package io.inprice.crawler.manager.resolver.uk;

import io.inprice.crawler.common.models.LinkSpec;
import io.inprice.crawler.manager.resolver.AbstractResolver;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ArgosResolver extends AbstractResolver {

    @Override
    public String getTitle() {
        String val = null;
        Element title = doc.selectFirst("span.product-title");
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
        BigDecimal val = null;
        Element price = doc.selectFirst(".product-price-primary");
        if (price != null) {
            val = new BigDecimal(price.attr("content").trim());
        }
        return val;
    }

    @Override
    public String getSeller() {
        String val = null;
        Element seller = doc.selectFirst(".product-brand a");
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
        return null;
    }

    @Override
    public List<LinkSpec> getSpecList() {
        List<LinkSpec> specList = null;
        Elements specs = doc.select(".product-description-content-text li");
        if (specs != null && specs.size() > 0) {
            specList = new ArrayList<>();
            for (Element spec : specs) {
                specList.add(new LinkSpec("", spec.text().trim()));
            }
        }
        return specList;
    }
}
