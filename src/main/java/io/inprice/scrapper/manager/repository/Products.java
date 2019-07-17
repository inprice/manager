package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.manager.helpers.DBUtils;
import io.inprice.scrapper.manager.info.ProductLinks;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Products {

    private static final Logger log = new Logger(Products.class);

    public static boolean updatePrice(Long prodId, BigDecimal prodPrice, int position, String minSeller, String maxSeller, BigDecimal minPrice, BigDecimal avgPrice, BigDecimal maxPrice) {
        return DBUtils.executeBatchQueries(new String[] {

            String.format(
                "insert into product_price " +
                "(product_id, price, position, min_seller, max_seller, min_price, avg_price, max_price) " +
                "values " +
                "(%d, %f, %d, '%s', '%s', %f, %f, %f);",
                prodId, prodPrice, position, minSeller, maxSeller, minPrice, avgPrice, maxPrice),

            String.format(
                "update product " +
                "set position=%d, min_seller='%s', max_seller='%s', min_price=%f, avg_price=%f, max_price=%f, last_update=now() " +
                "where id = %d ",
                position, minSeller, maxSeller, minPrice, avgPrice, maxPrice, prodId)

            }, String.format("Failed to update product price. Product Id: %d, Avg.Price: %f", prodId, avgPrice)

        );
    }

    public static List<ProductLinks> getProductLinks(Long productId) {
        return DBUtils.findMultiple(
            String.format(
                "select p.id as prod_id, p.price as prod_price, l.id as link_id, l.price as link_price, l.seller, s.name as site_name " +
                "from product as p " +
                "inner join link as l on l.product_id = p.id " +
                "inner join site as s on s.id = l.site_id " +
                "where p.id = %d " +
                "  and p.price > 0 " +
                "  and l.price > 0" +
                "  and l.status != '%s' " +
                "order by l.price ",
                productId, Status.PAUSED),
            Products::mapProductLinks
        );
    }

    private static ProductLinks mapProductLinks(ResultSet rs) {
        ProductLinks pl = new ProductLinks();
        try {
            pl.setId(rs.getLong("prod_id"));
            pl.setPrice(rs.getBigDecimal("prod_price"));
            pl.setLinkId(rs.getLong("link_id"));
            pl.setLinkPrice(rs.getBigDecimal("link_price"));
            pl.setSeller(rs.getString("seller"));
            pl.setSiteName(rs.getString("site_name"));

        } catch (SQLException e) {
            log.error("Failed to set product's properties", e);
        }
        return pl;
    }

}
