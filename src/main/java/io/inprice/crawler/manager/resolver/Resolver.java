package io.inprice.crawler.manager.resolver;

import io.inprice.crawler.common.models.Link;
import io.inprice.crawler.common.models.LinkSpec;

import java.math.BigDecimal;
import java.util.List;

public interface Resolver {

    Link resolve(String url, boolean print);

    String getTitle();

    String getCode();

    BigDecimal getPrice();

    String getSeller();

    String getShipment();

    String getBrand();

    List<LinkSpec> getSpecList();

}
