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

public class LinkRepository {

    private static final Logger log = LoggerFactory.getLogger(LinkRepository.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
    private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
    private static final Properties props = Beans.getSingleton(Properties.class);

    /**
     * This method can be used for both links and imported products at the same time.
     * isLookingForImportedProducts parameter is used to distinguish the searching direction between links and imported product rows
     *
     */
    public List<Link> getLinks(Status status, boolean isLookingForImportedProducts) {
        final String query = String.format(
                "select l.*, p.price as product_price from link as l " +
                "inner join workspace as ws on ws.id = l.workspace_id " +
                "inner join product as p on p.id = l.product_id " +
                "where l.status = '%s' " +
                "  and ws.active = true " +
                "  and ws.due_date >= now() " +
                "  and l.last_check < now() - interval 30 minute " + //last check time must be older than 30 minutes
                "  and l.import_row_id " + (isLookingForImportedProducts ? "is not null " : " is null ") +
                "limit %d",
                status.name(), props.getDB_FetchLimit());

        return findAll(query);
    }

    /**
     * This method can be used for both links and imported products at the same time.
     * isLookingForImportedProducts parameter is used to distinguish the searching direction between links and imported product rows
     *
     */
    public List<Link> getFailedLinks(Status status, int retryLimit, boolean isLookingForImportedProducts) {
        final String query = String.format(
                "select l.*, p.price as product_price from link as l " +
                "inner join workspace as ws on ws.id = l.workspace_id " +
                "inner join product as p on p.id = l.product_id " +
                "where l.status = '%s' " +
                "  and l.retry < %d " +
                "  and ws.active = true " +
                "  and ws.due_date >= now() " +
                "  and l.last_check < now() - interval 30 minute " + //last check time must be older than 30 minutes
                "  and l.import_row_id " + (isLookingForImportedProducts ? "is not null " : " is null ") +
                "limit %d",
                status.name(), retryLimit, props.getDB_FetchLimit());

        return findAll(query);
    }

    public void setLastCheckTime(String ids, boolean increaseRetry) {
        final String query =
                "update link " +
                "set last_check=now() " +
                (increaseRetry ? ", retry=retry+1 " : "") +
                "where id in (" + ids + ") ";

        dbUtils.executeQuery(query, "Failed to set last check time of ids: " + ids);
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
    public boolean makeAvailable(Link link) {
        boolean result = false;

        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            //if it is a normal link (not an imported product's link)
            if (link.getImportRowId() == null) {

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
                        executeSimpleQuery(con, "delete from link_spec where link_id=" + link.getId());

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

            //if it is an imported product's link
            } else {
                //no need to keep the link any more!
                try (PreparedStatement pst = con.prepareStatement("delete from link where id = " + link.getId())) {
                    pst.executeUpdate();
                    productRepository.createAProductFromLink(con, link);
                    updateImportRow(con, link.getImportRowId(), link.getStatus());
                } catch (Exception e) {
                    log.error("Failed to delete a link in make available method. Link Id: " + link.getId(), e);
                }
            }

            if (result) {
                dbUtils.commit(con);
            } else {
                dbUtils.rollback(con);
            }

        } catch (SQLException e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to make available a link. Link Id: " + link.getId(), e);
        } finally {
            if (con != null) dbUtils.close(con);
        }

        return result;
    }

    public boolean changeStatus(StatusChange change) {
        boolean result = false;

        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            final String oldStatusName = change.getOldStatus().name();
            final String newStatusName = change.getLink().getStatus().name();

            if (change.getLink().getHttpStatus() == null) change.getLink().setHttpStatus(0);

            //if it is a normal link (not an imported product's link)
            if (change.getLink().getImportRowId() == null) {

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

            //if it is an imported product's link
            } else {
                updateImportRow(con, change.getLink().getImportRowId(), change.getLink().getStatus());
            }

            if (result) {
                dbUtils.commit(con);
            } else {
                dbUtils.rollback(con);
            }

        } catch (SQLException e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to add a new status. Link Id: " + change.getLink().getId(), e);
        } finally {
            if (con != null) dbUtils.close(con);
        }

        return result;
    }

    public boolean changePrice(PriceUpdateInfo change) {
        boolean result = false;

        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

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
                dbUtils.commit(con);
            } else {
                dbUtils.rollback(con);
            }

        } catch (SQLException e) {
            dbUtils.rollback(con);
            log.error("Failed to change price. Link Id: {}, Price: {}", change.getLinkId(), change.getNewPrice(), e);
        } finally {
            if (con != null) dbUtils.close(con);
        }

        return result;
    }

    /**
     * Deletes all expired links added for imported products.
     */
    public void deleteImportedProductsLinks() {
        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            executeSimpleQuery(
                con,
        "delete from link " +
                "where last_check < now() - interval 5 day " + //last check time must be older than 5 days
                "  and import_row_id is not null"
            );

            con.commit();

        } catch (SQLException e) {
            dbUtils.rollback(con);
            log.error("Failed to delete all expired links for imported products!", e);
        } finally {
            if (con != null) dbUtils.close(con);
        }
    }

    private void addStatusChangeHistory(Connection con, Link link) {
        executeSimpleQuery(
            con,
            String.format(
                "insert into link_history (link_id, status, http_status, company_id, workspace_id, product_id) " +
                "values (%d, '%s', %d, %d, %d, %d)",
                link.getId(), link.getStatus(), link.getHttpStatus(), link.getCompanyId(), link.getWorkspaceId(), link.getProductId()
            )
        );
    }

    private void addPriceChangeHistory(Connection con, Link link) {
        addPriceChangeHistory(con, link.getId(), link.getPrice(), link.getCompanyId(), link.getWorkspaceId(), link.getProductId());
    }

    private void addPriceChangeHistory(Connection con, PriceUpdateInfo priceInfo) {
        addPriceChangeHistory(con, priceInfo.getLinkId(), priceInfo.getNewPrice(), priceInfo.getCompanyId(), priceInfo.getWorkspaceId(), priceInfo.getProductId());
    }

    private void addPriceChangeHistory(Connection con, long linkId, BigDecimal price, long companyId, long workspaceId, long productId) {
        executeSimpleQuery(
            con,
            String.format(
            "insert into link_price (link_id, price, companyId, workspaceId, productId) " +
                "values (%d, %f, %d, %d, %d)", linkId, price, companyId, workspaceId, productId)
        );
    }

    private void executeSimpleQuery(Connection con, String query) {
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.executeUpdate();
        } catch (Exception e) {
            log.error("Failed to execute query: " + query, e);
        }
    }

    private void updateImportRow(Connection con, Long importRowId, Status status) {
        try (PreparedStatement pst = con.prepareStatement("update import_product_row set status=? where id = ?")) {
            int i = 0;
            pst.setString(++i, status.name());
            pst.setLong(++i, importRowId);

            boolean updated = (pst.executeUpdate() > 0);
            if (! updated) {
                log.warn("Failed to set an imported product status. Id: " +importRowId);
            }
        } catch (Exception e) {
            log.error("Failed to set an imported product status. Id: " +importRowId, e);
        }
    }

    private List<Link> findAll(String query) {
        return dbUtils.findMultiple(query, this::map);
    }

    private Link map(ResultSet rs) {
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

            //is an imported product!
            model.setImportId(rs.getLong("import_id"));
            model.setImportRowId(rs.getLong("import_row_id"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set link's properties", e);
        }
        return null;
    }

}
