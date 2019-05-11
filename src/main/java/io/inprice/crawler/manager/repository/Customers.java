package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.Country;
import io.inprice.crawler.common.models.Customer;
import io.inprice.crawler.common.models.Sector;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class Customers {

    private static final Logger log = new Logger(Customers.class);

    private static final String PLAIN_SEARCH_QUERY = "select * from customer ";

    public static List<Customer> getAll() {
        return findAll(String.format("%s where active = true ", PLAIN_SEARCH_QUERY));
    }

    public static List<Customer> search(String term) {
        return findAll(PLAIN_SEARCH_QUERY + " where active = true and title like '%" + term + "%'");
    }

    public static Customer getOne(Long id) {
        List<Customer> list = findAll(String.format("%s where id = %d", PLAIN_SEARCH_QUERY, id));
        if (list != null && list.size() > 0)
            return list.get(0);
        else
            return null;
    }

    private static List<Customer> findAll(String query) {
        List<Customer> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Error in fetching Customers", e);
        }

        return result;
    }

    private static Customer map(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setId(rs.getLong("id"));
        customer.setEmail(rs.getString("email"));
        customer.setPasswordHash(rs.getString("password_hash"));
        customer.setPasswordSalt(rs.getString("password_salt"));
        customer.setTitle(rs.getString("title"));
        customer.setContactName(rs.getString("contact_name"));
        customer.setWebsite(rs.getString("website"));
        customer.setAddress(rs.getString("address"));
        customer.setInsertAt(rs.getDate("insert_at"));

        customer.setSectorId(rs.getLong("sector_id"));
        customer.setCountryId(rs.getLong("country_id"));

        return customer;
    }
}
