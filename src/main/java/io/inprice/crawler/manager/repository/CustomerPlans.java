package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.*;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerPlans {

    private static final Logger log = new Logger(CustomerPlans.class);

    private static final String PLAIN_SEARCH_QUERY = "select * from customer_plan ";

    public static List<CustomerPlan> getAll(Long customerId) {
        return findAll(String.format("%s where active = true and customer_id = %d", PLAIN_SEARCH_QUERY, customerId));
    }

    public static CustomerPlan getOne(Long id) {
        List<CustomerPlan> list = findAll(String.format("%s where id = %d", PLAIN_SEARCH_QUERY, id));
        if (list != null && list.size() > 0)
            return list.get(0);
        else
            return null;
    }

    private static List<CustomerPlan> findAll(String query) {
        List<CustomerPlan> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Error in fetching CustomerPlans", e);
        }

        return result;
    }

    private static CustomerPlan map(ResultSet rs) throws SQLException {
        CustomerPlan customerPlan = new CustomerPlan();
        customerPlan.setId(rs.getLong("id"));
        customerPlan.setActive(rs.getBoolean("active"));
        customerPlan.setMonthly(rs.getBoolean("monthly"));
        customerPlan.setDueDate(rs.getDate("due_date"));
        customerPlan.setCollectingTime(rs.getDate("collecting_time"));
        customerPlan.setCollectingOK(rs.getBoolean("collecting_ok"));
        customerPlan.setCollectingRetries(rs.getInt("collecting_retries"));
        customerPlan.setInsertAt(rs.getDate("insert_at"));

        customerPlan.setCustomerId(rs.getLong("customer_id"));
        customerPlan.setBrandId(rs.getLong("brand_id"));
        customerPlan.setPlanId(rs.getLong("plan_id"));

        return customerPlan;
    }
}
