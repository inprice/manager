package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.info.LinkStatusChange;
import io.inprice.scrapper.common.info.PriceChange;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Links {

    private static final Logger log = new Logger(Links.class);

    public static synchronized boolean updateCycleValues(LinkStatus linkStatus, int cycle, String ids, boolean willRetryBeIncremented) {
        final String incrementPart = (willRetryBeIncremented ? ", retry = retry + 1" : "");

        return executeQuery(
            String.format(
                "update link " +
                "set cycle = %d, last_check = now() " + incrementPart +
                "where status = '%s' " +
                "  and cycle <> %d " +
                "  and id in (%s) ",
                cycle, linkStatus.name(), cycle, ids),

            String.format("Failed to change %s links cycle %d", linkStatus.name(), cycle)
        );
    }

    public static List<Link> getLinks(LinkStatus linkStatus, int cycle) {
        final String query = String.format(
                "select * from link " +
                "inner join customer_plan as cp on cp.id = customer_plan_id " +
                "where status = '%s'" +
                "  and cycle <> %d " +
                "  and cp.active = true " +
                "  and cp.due_date >= now() " +
                "limit %d ",
                linkStatus.name(), cycle, Config.DB_FETCH_LIMIT);

        return findAll(query);
    }

    public static List<Link> getFailedLinks(LinkStatus linkStatus, int cycle, int retryLimit) {
        final String query = String.format(
                "select * from link " +
                "inner join customer_plan as cp on cp.id = customer_plan_id " +
                "where status = '%s'" +
                "  and cycle <> %d " +
                "  and retry <  %d " +
                "  and cp.active = true " +
                "  and cp.due_date >= now() " +
                "limit %d ",
                linkStatus.name(), cycle, retryLimit, Config.DB_FETCH_LIMIT);

        return findAll(query);
    }

    public static boolean setSiteAndWebsiteClassName(Long linkId, Long siteId, String websiteClassName) {
        return executeQuery(
            String.format(
                "update link " +
                "set site_id = %d, website_class_name = '%s', last_update = now() " +
                "where link_id = %d ",
                siteId, websiteClassName, linkId),

            String.format("Failed to set site_id and website_class_name. Link Id: %d, Site Id: %d, Class Name: %s", linkId, siteId, websiteClassName)
        );
    }

    public static boolean changeStatus(LinkStatusChange change) {
        final String notePart = (change.getNote() != null ? ", note = '" + change.getNote() +"' " : "");

        return executeBatchQueries(new String[] {

            String.format(
                "insert into link_history " +
                "(link_id, status, http_status) " +
                "values " +
                "(%d, %s, %d);",
                change.getLinkId(), change.getNewStatus().name(), change.getHttpStatus()),

            String.format(
                "update link " +
                "set status = '%s', http_status = %d " + notePart +
                "where link_id = %d ",
                change.getNewStatus().name(), change.getHttpStatus(), change.getLinkId())

            }, String.format("Failed to change status. Link Id: %d, Old Status: %s, New Status: %s", change.getLinkId(), change.getOldStatus(), change.getNewStatus())

        );
    }

    public static boolean changePrice(PriceChange change) {
        return executeBatchQueries(new String[] {

            String.format(
                "insert into link_price " +
                "(link_id, price) " +
                "values " +
                "(%d, %f);",
                change.getLinkId(), change.getNewPrice()),

            String.format(
                "update link " +
                "set price = %f, last_update = now() " +
                "where link_id = %d ",
                change.getNewPrice(), change.getLinkId())

            }, String.format("Failed to change price. Link Id: %d, Price: %f", change.getLinkId(), change.getNewPrice())

        );
        //TODO: linkin min, avg ve max fiyatlarla position bilgileri bu kisimda update edilmeli!!!
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
        link.setBrand(rs.getString("brand"));
        link.setSeller(rs.getString("seller"));
        link.setShipment(rs.getString("shipment"));
        link.setPrice(rs.getBigDecimal("price"));
        link.setLastCheck(rs.getDate("last_check"));
        link.setLastUpdate(rs.getDate("last_update"));
        link.setCycle(rs.getInt("cycle"));
        link.setStatus(LinkStatus.valueOf(rs.getString("status")));
        link.setRetry(rs.getInt("retry"));
        link.setNote(rs.getString("note"));

        link.setCustomerId(rs.getLong("customer_id"));
        link.setCustomerPlanId(rs.getLong("customer_plan_id"));
        link.setProductId(rs.getLong("product_id"));
        link.setSiteId(rs.getLong("site_id"));

        link.setWebsiteClassName(rs.getString("website_class_name"));

        return link;
    }

    private static boolean executeQuery(String query, String errorMessage) {
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            log.error(errorMessage, e);
        }
        return false;
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

}
