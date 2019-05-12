package io.inprice.scrapper.manager.websites.uk;

import io.inprice.scrapper.common.models.LinkSpec;
import io.inprice.scrapper.manager.websites.AbstractWebsite;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Argos extends AbstractWebsite {

    @Override
    public boolean isAvailable() {
        return super.isAvailable();
    }

    @Override
    public String getCode() {
        String val = null;
        Element code = doc.selectFirst("[itemProp='sku']");
        if (code != null) {
            val = code.attr("content").trim();
        }
        return val;
    }

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
        return "Argos";
    }

    @Override
    public String getShipment() {
        return "Argos";
    }

    @Override
    public String getBrand() {
        String val = null;
        Element seller = doc.selectFirst(".product-brand a");
        if (seller != null) {
            val = seller.text().trim();
        }
        return val;
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
