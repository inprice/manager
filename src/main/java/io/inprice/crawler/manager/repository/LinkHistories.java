package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.meta.LinkStatus;
import io.inprice.crawler.common.models.LinkHistory;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LinkHistories {

    private static final Logger log = new Logger(LinkHistories.class);

    public static List<LinkHistory> getAll(Long linkId) {
        final String query = String.format("select * from link_history where link_id = %d", linkId);

        List<LinkHistory> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                LinkHistory linkHistory = new LinkHistory();
                linkHistory.setId(rs.getLong("id"));
                if (rs.getString("status") != null) {
                    linkHistory.setStatus(LinkStatus.valueOf(rs.getString("status")));
                }
                linkHistory.setInsertAt(rs.getDate("insert_at"));

                result.add(linkHistory);
            }
        } catch (Exception e) {
            log.error("Error in fetching LinkHistories", e);
        }

        return result;
    }

    public static boolean add(Long linkId, String status) {
        final String query =
                String.format("insert into link_history (link_id, status) " +
                        "values (%d, %s) ",
                        linkId, status);

        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            log.error("Error in adding a new LinkHistory", e);
        }

        return false;
    }

}
