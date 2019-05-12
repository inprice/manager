package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.DBUtils;
import io.inprice.scrapper.manager.helpers.Global;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Links {

    private static final Logger log = new Logger(Links.class);

    public static boolean updateCycleValues(String ids) {
        String query = String.format("update link " +
                "set cycle = %d " +
                "where id in (%s) " +
                "  and cycle != %d",
                Global.cycle, ids, Global.cycle);

        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            log.error("Error in changing Link's cycle numbers", e);
        }

        return false;
    }

    public static List<Link> getActiveSites() {
        String query = String.format("select * from link " +
                "inner join customer_plan as cp cp.id = customer_plan_id " +
                "where status = '%s'" +
                "  and cycle != %d'   " +
                "  and cp.active = true " +
                "  and cp.due_date >= now() " +
                "limit %d ",
                LinkStatus.ACTIVE, Global.cycle, Config.DB_FETCH_LIMIT);

        return findAll(query);
    }

    private static List<Link> findAll(String query) {
        List<Link> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Failed to fetch Links", e);
        }

        return result;
    }

    private static Link map(ResultSet rs) throws SQLException {
        Link link = new Link(rs.getString("url"));
        link.setId(rs.getLong("id"));
        link.setTitle(rs.getString("title"));
        link.setCode(rs.getString("code"));
        link.setAltUrl(rs.getString("alt_url"));
        link.setBrand(rs.getString("brand"));
        link.setSeller(rs.getString("seller"));
        link.setShipment(rs.getString("shipment"));
        link.setPrice(rs.getBigDecimal("price"));
        link.setCycle(rs.getInt("cycle"));
        link.setNote(rs.getString("note"));

        if (rs.getString("status") != null) {
            link.setStatus(LinkStatus.valueOf(rs.getString("status")));
        } else {
            link.setStatus(LinkStatus.UNKNOWN);
        }

        link.setCustomerPlanId(rs.getLong("customer_plan_id"));
        link.setProductId(rs.getLong("product_id"));
        link.setSiteId(rs.getLong("site_id"));

        return link;
    }

}
