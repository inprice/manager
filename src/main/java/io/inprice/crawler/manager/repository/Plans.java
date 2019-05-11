package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.Plan;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Plans {

    private static final Logger log = new Logger(Plans.class);

    private static final String PLAIN_SEARCH_QUERY = "select * from plan ";

    public static List<Plan> getAll() {
        return findAll(String.format("%s where active = true order by order_no", PLAIN_SEARCH_QUERY));
    }

    public static Plan getOne(Long id) {
        List<Plan> list = findAll(String.format("%s where id = %d", PLAIN_SEARCH_QUERY, id));
        if (list != null && list.size() > 0)
            return list.get(0);
        else
            return null;
    }

    private static List<Plan> findAll(String query) {
        List<Plan> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Error in fetching Plans", e);
        }

        return result;
    }

    private static Plan map(ResultSet rs) throws SQLException {
        Plan plan = new Plan();
        plan.setId(rs.getLong("id"));
        plan.setActive(rs.getBoolean("active"));
        plan.setName(rs.getString("name"));
        plan.setDesc1(rs.getString("desc_1"));
        plan.setDesc2(rs.getString("desc_2"));
        plan.setDesc3(rs.getString("desc_3"));
        plan.setRowLimit(rs.getInt("row_limit"));
        plan.setPrice(rs.getBigDecimal("price"));
        plan.setPrice1(rs.getBigDecimal("price_1"));
        plan.setOrderNo(rs.getInt("order_no"));
        plan.setCssClass(rs.getString("css_class"));
        plan.setFree(rs.getBoolean("free"));

        return plan;
    }
}
