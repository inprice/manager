package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.info.LinkStatusChange;
import io.inprice.scrapper.common.info.PriceChange;
import io.inprice.scrapper.common.info.ProductPriceInfo;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Product;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.manager.helpers.DBUtils;
import io.inprice.scrapper.manager.helpers.RedisClient;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Products {

    private static final Logger log = new Logger(Products.class);

    public static boolean updatePrice(ProductPriceInfo ppi, int position, String minSeller, String maxSeller, BigDecimal minPrice, BigDecimal avgPrice, BigDecimal maxPrice) {
        return executeBatchQueries(new String[] {

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
        final String query = String.format("select * from product where id = %d ", id);

        Product result = null;
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) result = map(rs);
        } catch (Exception e) {
            log.error("Failed to find Product by id: " + id, e);
        }

        return result;
    }

    private static boolean executeBatchQueries(String[] queries, String errorMessage) {
        boolean result = false;

        Connection con = null;
        Statement sta = null;
        try {
            con = DBUtils.getTransactionalConnection();
            sta = con.createStatement();

            for (String query: queries) {
                sta.addBatch(query);
            }

            int[] affected = sta.executeBatch();

            result = true;
            for (int aff: affected) {
                if (aff < 1) {
                    DBUtils.rollback(con);
                    result = false;
                    break;
                }
            }
        } catch (SQLException e) {
            DBUtils.rollback(con);
            log.error(errorMessage, e);
        } finally {
            DBUtils.close(con, sta);
            return result;
        }
    }

    private static Product map(ResultSet rs) throws SQLException {
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
    }

}
