package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.PriceUpdateInfo;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.LinkSpec;
import io.inprice.scrapper.manager.config.Properties;
import io.inprice.scrapper.manager.helpers.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Links {

    private static final Logger log = LoggerFactory.getLogger(Links.class);
    private static final Properties props = Beans.getSingleton(Properties.class);

    public static List<Link> getLinks(Status status) {
        final String query = String.format(
                "select l.*, p.price as product_price from link as l " +
                "inner join workspace as ws on ws.id = l.workspace_id " +
                "inner join product as p on p.id = l.product_id " +
                "where l.status = '%s' " +
                "  and ws.active = true " +
                "  and ws.due_date >= now() " +
                "  and l.last_check < now() - interval 30 minute " + //last check time must be older than 30 minutes
                "limit %d",
                status.name(), props.getDB_FetchLimit());

        return findAll(query);
    }

    public static List<Link> getFailedLinks(Status status, int retryLimit) {
        final String query = String.format(
                "select l.*, p.price as product_price from link as l " +
                "inner join workspace as ws on ws.id = l.workspace_id " +
                "inner join product as p on p.id = l.product_id " +
                "where l.status = '%s' " +
                "  and l.retry < %d " +
                "  and ws.active = true " +
                "  and ws.due_date >= now() " +
                "  and l.last_check < now() - interval 30 minute " + //last check time must be older than 30 minutes
                "limit %d",
                status.name(), retryLimit, props.getDB_FetchLimit());

        return findAll(query);
    }

    public static void setLastCheckTime(String ids, boolean increaseRetry) {
        final String query =
                "update link " +
                "set last_check=now() " +
                (increaseRetry ? ", retry=retry+1 " : "") +
                "where id in (" + ids + ") ";

        DBUtils.executeQuery(query, "Failed to set last check time of ids: " + ids);
    }

    /**
     * A link which is in NEW status becomes AVAILABLE with the help of this method.
     * This method does several database operations, please see below;
     *
     *  - All the basic information of the link is set first
     *  - In order to add a status change into link_history table, changeStatus method is called
     *  - In order to add a price change into link_price table, changePrice method is called
     *  - Specs of the link are added
     *
     * @return boolean
     */
    public static boolean makeAvailable(Link link) {
        boolean result = false;

        Connection con = null;
        try {
            con = DBUtils.getTransactionalConnection();

            final String q1 =
                "update link " +
                "set name=?, sku=?, brand=?, seller=?, shipment=?, price=?, status=?, " +
                    "previous_status=?, site_id=?, website_class_name=?, last_update=now(), retry=0, http_status=0 " +
                "where id = ? " +
                "  and status != ?";

            try (PreparedStatement pst = con.prepareStatement(q1)) {
                int i = 0;
                pst.setString(++i, link.getName());
                pst.setString(++i, link.getSku());
                pst.setString(++i, link.getBrand());
                pst.setString(++i, link.getSeller());
                pst.setString(++i, link.getShipment());
                pst.setBigDecimal(++i, link.getPrice());
                pst.setString(++i, link.getStatus().name());
                pst.setString(++i, link.getPreviousStatus().name());
                pst.setLong(++i, link.getSiteId());
                pst.setString(++i, link.getWebsiteClassName());
                pst.setLong(++i, link.getId());
                pst.setString(++i, link.getStatus().name());

                result = (pst.executeUpdate() > 0);
            } catch (Exception e) {
                log.error("Failed to make a link available. Link Id: " + link.getId(), e);
            }

            if (result) {
                addStatusChangeHistory(con, link);
                addPriceChangeHistory(con, link);

                if (link.getSpecList() != null && link.getSpecList().size() > 0) {
                    //deleting old specs if any
                    executeSimpleQuery(con,"delete from link_spec where link_id=" + link.getId());

                    int j;
                    final String q3 = "insert into link_spec (link_id, _key, _value) values (?, ?, ?)";
                    try (PreparedStatement pst = con.prepareStatement(q3)) {
                        for (int i = 0; i < link.getSpecList().size(); i++) {
                            LinkSpec spec = link.getSpecList().get(i);

                            j = 0;
                            pst.setLong(++j, link.getId());
                            pst.setString(++j, spec.getKey());
                            pst.setString(++j, spec.getValue());
                            pst.addBatch();
                        }
                        pst.executeBatch();
                    }
                }
            } else {
                log.warn("Link is already in {} status. Link Id: {} ", link.getStatus().name(), link.getId());
            }

            if (result) {
                DBUtils.commit(con);
            } else {
                DBUtils.rollback(con);
            }

        } catch (SQLException e) {
            if (con != null) DBUtils.rollback(con);
            log.error("Failed to make available a link. Link Id: " + link.getId(), e);
        } finally {
            if (con != null) DBUtils.close(con);
        }

        return result;
    }

    public static boolean changeStatus(StatusChange change) {
        boolean result = false;

        Connection con = null;
        try {
            con = DBUtils.getTransactionalConnection();

            final String oldStatusName = change.getOldStatus().name();
            final String newStatusName = change.getLink().getStatus().name();

            if (change.getLink().getHttpStatus() == null) change.getLink().setHttpStatus(0);

            final String q1 =
                "update link " +
                "set status=?, previous_status=?, http_status=?, last_update=now() " +
                (change.getLink().getHttpStatus() != 0 ? ", retry=retry+1 " : "") +
                "where id=? " +
                "  and status!=?";

            try (PreparedStatement pst = con.prepareStatement(q1)) {
                int i = 0;
                pst.setString(++i, newStatusName);
                pst.setString(++i, oldStatusName);
                pst.setInt(++i, change.getLink().getHttpStatus());
                pst.setLong(++i, change.getLink().getId());
                pst.setString(++i, newStatusName);

                result = (pst.executeUpdate() > 0);
            }

            if (result) {
                addStatusChangeHistory(con, change.getLink());
            } else {
                log.warn("Link's status is already changed! Link Id: {}, Old Status: {}, New Status: {}",
                        change.getLink().getId(), oldStatusName, newStatusName);
            }

            if (result) {
                DBUtils.commit(con);
            } else {
                DBUtils.rollback(con);
            }

        } catch (SQLException e) {
            if (con != null) DBUtils.rollback(con);
            log.error("Failed to add a new status. Link Id: " + change.getLink().getId(), e);
        } finally {
            if (con != null) DBUtils.close(con);
        }

        return result;
    }

    public static boolean changePrice(PriceUpdateInfo change) {
        boolean result = false;

        Connection con = null;
        try {
            con = DBUtils.getTransactionalConnection();

            final String q1 =
                "update link " +
                "set price=?, last_update=now() " +
                "where id=? " +
                "  and price<>?";

            try (PreparedStatement pst = con.prepareStatement(q1)) {
                int i = 0;
                pst.setBigDecimal(++i, change.getNewPrice());
                pst.setLong(++i, change.getLinkId());
                pst.setBigDecimal(++i, change.getNewPrice());
                result = (pst.executeUpdate() > 0);
            } catch (Exception e) {
                log.error("Failed to change price of a link at step 1. Link Id: " + change.getLinkId(), e);
            }

            if (result) {
                addPriceChangeHistory(con, change);
                DBUtils.commit(con);
            } else {
                DBUtils.rollback(con);
            }

        } catch (SQLException e) {
            DBUtils.rollback(con);
            log.error("Failed to change price. Link Id: {}, Price: {}", change.getLinkId(), change.getNewPrice(), e);
        } finally {
            if (con != null) DBUtils.close(con);
        }

        return result;
    }

    private static void addStatusChangeHistory(Connection con, Link link) {
        executeSimpleQuery(
            con,
            String.format(
                "insert into link_history (link_id, status, http_status, company_id, workspace_id, product_id) " +
                "values (%d, '%s', %d, %d, %d, %d)",
                link.getId(), link.getStatus(), link.getHttpStatus(), link.getCompanyId(), link.getWorkspaceId(), link.getProductId()
            )
        );
    }

    private static void addPriceChangeHistory(Connection con, Link link) {
        addPriceChangeHistory(con, link.getId(), link.getPrice(), link.getCompanyId(), link.getWorkspaceId(), link.getProductId());
    }

    private static void addPriceChangeHistory(Connection con, PriceUpdateInfo priceInfo) {
        addPriceChangeHistory(con, priceInfo.getLinkId(), priceInfo.getNewPrice(), priceInfo.getCompanyId(), priceInfo.getWorkspaceId(), priceInfo.getProductId());
    }

    private static void addPriceChangeHistory(Connection con, long linkId, BigDecimal price, long companyId, long workspaceId, long productId) {
        executeSimpleQuery(
            con,
            String.format(
            "insert into link_price (link_id, price, companyId, workspaceId, productId) " +
                "values (%d, %f, %d, %d, %d)", linkId, price, companyId, workspaceId, productId)
        );
    }

    private static void executeSimpleQuery(Connection con, String query) {
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.executeUpdate();
        } catch (Exception e) {
            log.error("Failed to execute query: " + query, e);
        }
    }

    private static List<Link> findAll(String query) {
        return DBUtils.findMultiple(query, Links::map);
    }

    private static Link map(ResultSet rs) {
        try {
            Link model = new Link(rs.getString("url"));
            model.setId(rs.getLong("id"));
            model.setName(rs.getString("name"));
            model.setSku(rs.getString("sku"));
            model.setBrand(rs.getString("brand"));
            model.setSeller(rs.getString("seller"));
            model.setShipment(rs.getString("shipment"));
            model.setPrice(rs.getBigDecimal("price"));
            model.setLastCheck(rs.getDate("last_check"));
            model.setLastUpdate(rs.getDate("last_update"));
            model.setStatus(Status.valueOf(rs.getString("status")));
            model.setPreviousStatus(Status.valueOf(rs.getString("previous_status")));
            model.setRetry(rs.getInt("retry"));
            model.setWebsiteClassName(rs.getString("website_class_name"));

            model.setCompanyId(rs.getLong("company_id"));
            model.setWorkspaceId(rs.getLong("workspace_id"));
            model.setProductId(rs.getLong("product_id"));
            model.setSiteId(rs.getLong("site_id"));

            model.setProductPrice(rs.getBigDecimal("product_price"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set link's properties", e);
        }
        return null;
    }

}
