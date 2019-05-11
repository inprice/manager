package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.Country;
import io.inprice.crawler.common.models.Customer;
import io.inprice.crawler.common.models.CustomerBrand;
import io.inprice.crawler.common.models.Sector;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerBrands {

    private static final Logger log = new Logger(CustomerBrands.class);

    public static List<CustomerBrand> getAll(Long customerId) {
        final String query =
                String.format("select * from customer_brand where customer_id = %d order by name", customerId);

        List<CustomerBrand> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Error in fetching CustomerBrands", e);
        }

        return result;
    }

    public static CustomerBrand getOne(Long id) {
        final String query = String.format("select * from customer_brand where id = %d", id);

        CustomerBrand result = null;
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                result = map(rs);
            }
        } catch (Exception e) {
            log.error("Error in fetching CustomerBrand by id", e);
        }

        return result;
    }

    private static CustomerBrand map(ResultSet rs) throws SQLException {
        CustomerBrand customerBrand = new CustomerBrand();
        customerBrand.setId(rs.getLong("id"));
        customerBrand.setName(rs.getString("name"));
        customerBrand.setCustomerId(rs.getLong("customer_id"));

        return customerBrand;
    }
}
