package io.inprice.scrapper.manager.websites.xx;

import io.inprice.scrapper.common.models.LinkSpec;
import io.inprice.scrapper.manager.websites.AbstractWebsite;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * itemid den buldurma : https://www.ebay.de/itm/372661939240
 */
public class Ebay extends AbstractWebsite {

    @Override
    public boolean isAvailable() {
        Element stock = doc.getElementById("vi-quantity__select-box");
        if (stock != null) return true;

        stock = doc.selectFirst("#qtySubTxt span");
        if (stock != null) {
            String number = cleanPrice(stock.html().trim());
            try {
                int amount = new Integer(number.trim());
                System.out.println("amount : " + amount);
                return (amount > 0);
            } catch (Exception e) { }
        }
        return false;
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
    public String getTitle() {
        String val = null;
        Element title = doc.selectFirst("h1.product-title");
        if (title != null) {
            val = title.text().trim();
        } else {
            title = doc.selectFirst("a[data-itemid]");
            if (title != null) {
                val = title.attr("etafsharetitle").trim();
            }
        }
        return val;
    }

    @Override
    public BigDecimal getPrice() {
        String strPrice = null;

        Element price = doc.getElementById("prcIsum");
        if (price == null) price = doc.getElementById("prcIsum_bidPrice");

        if (price != null) {
            strPrice = price.attr("content").trim();
        } else {
            price = doc.select("#mm-saleDscPrc").first();
            if (price != null) strPrice = price.text().trim();
        }

        if (price == null) {
            price = doc.selectFirst("div.price");
            if (price != null) strPrice = price.text().trim();
        }

        if (strPrice == null)
            return BigDecimal.ZERO;
        else
            return new BigDecimal(cleanPrice(strPrice));
    }

    @Override
    public String getSeller() {
        String val = null;
        Element seller = doc.getElementById("mbgLink");
        if (seller != null) {
            String[] sellerChunks = seller.attr("aria-label").split(":");
            if (sellerChunks.length > 1) {
                val = sellerChunks[1].trim();
            }
        } else {
            seller = doc.selectFirst("div.seller-persona a");
            if (seller != null) {
                val = seller.text().trim();
            }
        }
        return val;
    }

    @Override
    public String getShipment() {
        String val = null;
        Element shipment = doc.selectFirst("#shSummary span");
        if (shipment == null) shipment = doc.selectFirst("span.logistics-cost");

        if (shipment != null) {
            val = shipment.text().trim();
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
        } else {
            specs = doc.select("#ProductDetails li div");
            if (specs != null && specs.size() > 0) {
                specList = new ArrayList<>();
                for (int i = 0; i < specs.size(); i++) {
                    String key = specs.get(i).text().trim();
                    String value = "";
                    if (i < specs.size() - 1) {
                        value = specs.get(++i).text().trim();
                    }
                    specList.add(new LinkSpec(key, value));
                }
            }
        }
        return specList;
    }
}
