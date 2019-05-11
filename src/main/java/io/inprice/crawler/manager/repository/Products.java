package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.CustomerBrand;
import io.inprice.crawler.common.models.CustomerPlan;
import io.inprice.crawler.common.models.Plan;
import io.inprice.crawler.common.models.Product;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Products {

    private static final Logger log = new Logger(Products.class);

    private static final String PLAIN_SEARCH_QUERY = "select * from product ";

    public static List<Product> getAll(Long customerPlanId) {
        return findAll(String.format("%s where active = true and customer_plan_id = %d", PLAIN_SEARCH_QUERY, customerPlanId));
    }

    public static Product getOne(Long id) {
        List<Product> list = findAll(String.format("%s where p.id = %d", PLAIN_SEARCH_QUERY, id));
        if (list != null && list.size() > 0)
            return list.get(0);
        else
            return null;
    }

    private static List<Product> findAll(String query) {
        List<Product> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Error in fetching Products", e);
        }

        return result;
    }

    private static Product map(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setActive(rs.getBoolean("active"));
        product.setCode(rs.getString("code"));
        product.setTitle(rs.getString("title"));
        product.setBrand(rs.getString("brand"));
        product.setCategory(rs.getString("category"));
        product.setPrice(rs.getBigDecimal("price"));

        product.setPlanId(rs.getLong("customer_plan_id"));

        return product;
    }
}
