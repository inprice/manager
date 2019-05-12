package io.inprice.scrapper.manager.websites.tr;

import io.inprice.scrapper.common.models.LinkSpec;
import io.inprice.scrapper.manager.websites.AbstractWebsite;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class HepsiBurada extends AbstractWebsite {

    @Override
    public boolean isAvailable() {
        Element available = doc.selectFirst("div.product-detail-box[style*='display: none']");
        return (available != null);
    }

    @Override
    public String getCode() {
        String val = null;
        Element code = doc.selectFirst("#addToCartForm input[name='sku']");
        if (code != null) {
            val = code.val().trim();
        }
        return val;
    }

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
        Element seller = doc.selectFirst("input[name='merchantId']");
        if (seller != null) {
            val = seller.val().trim();
        }
        return val;
    }

    @Override
    public String getShipment() {
        return "50 TL ve üzeri Kargo Bedava";
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
