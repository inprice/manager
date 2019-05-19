package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.info.ProductPriceInfo;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Product;
import io.inprice.scrapper.manager.helpers.DBUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Products {

    private static final Logger log = new Logger(Products.class);

    public static boolean updatePrice(ProductPriceInfo ppi, int position, String minSeller, String maxSeller, BigDecimal minPrice, BigDecimal avgPrice, BigDecimal maxPrice) {
        return DBUtils.executeBatchQueries(new String[] {

            String.format(
                "insert into product_price " +
                "(product_id, price, position, min_seller, max_seller, min_price, avg_price, max_price) " +
                "values " +
                "(%d, %f, %d, '%s', '%s', %f, %f, %f);",
                ppi.getProductId(), ppi.getPrice(), position, minSeller, maxSeller, minPrice, avgPrice, maxPrice),

            String.format(
                "update product " +
                "set price = %f, position = %d, min_seller = '%s', max_seller = '%s', min_price = %f, avg_price = %f, max_price = %f, last_update = now() " +
                "where product_id = %d ",
                ppi.getPrice(), position, minSeller, maxSeller, minPrice, avgPrice, maxPrice, ppi.getProductId())

            }, String.format("Failed to update product price. Product Id: %d, Price: %f", ppi.getProductId(), ppi.getPrice())

        );
    }

    public static Product findById(Long id) {
        return DBUtils.findSingle(
            String.format("select * from product where id = %d", id),
            Products::map
        );
    }

    private static Product map(ResultSet rs) {
        try {
            Product model = new Product();
            model.setId(rs.getLong("id"));
            model.setActive(rs.getBoolean("active"));
            model.setName(rs.getString("name"));
            model.setCode(rs.getString("code"));
            model.setBrand(rs.getString("brand"));
            model.setCategory(rs.getString("category"));
            model.setPrice(rs.getBigDecimal("price"));
            model.setPosition(rs.getInt("position"));
            model.setLastUpdate(rs.getDate("last_update"));
            model.setMinSeller(rs.getString("min_seller"));
            model.setMaxSeller(rs.getString("max_seller"));
            model.setMinPrice(rs.getBigDecimal("min_price"));
            model.setAvgPrice(rs.getBigDecimal("avg_price"));
            model.setMaxPrice(rs.getBigDecimal("max_price"));

            model.setCustomerPlanId(rs.getLong("customer_plan_id"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set product's properties", e);
        }
        return null;
    }

}
