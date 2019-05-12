package io.inprice.scrapper.manager.websites;

import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.LinkSpec;

import java.math.BigDecimal;
import java.util.List;

public interface Website {

    void check(Link link);

    boolean isAvailable();

    String getCode();

    String getTitle();

    BigDecimal getPrice();

    String getSeller();

    String getShipment();

    String getBrand();

    List<LinkSpec> getSpecList();

}
