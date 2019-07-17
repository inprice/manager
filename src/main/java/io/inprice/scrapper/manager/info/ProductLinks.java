package io.inprice.scrapper.manager.info;

import io.inprice.scrapper.common.models.Model;

import java.math.BigDecimal;

public class ProductLinks extends Model {

    private BigDecimal price;
    private Long linkId;
    private BigDecimal linkPrice;
    private String seller;
    private String siteName;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public BigDecimal getLinkPrice() {
        return linkPrice;
    }

    public void setLinkPrice(BigDecimal linkPrice) {
        this.linkPrice = linkPrice;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
}
