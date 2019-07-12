package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.info.PriceChange;
import io.inprice.scrapper.common.info.ProductPriceInfo;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.LinkSpec;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Links {

    private static final Logger log = new Logger(Links.class);

    public static List<Link> getLinks(Status status) {
        final String query = String.format(
                "select *, p.price as product_price from link " +
                "inner join customer_plan as cp on cp.id = customer_plan_id " +
                "inner join product as p on p.id = product_id " +
                "where status = '%s'" +
                "  and cp.active = true " +
                "  and cp.due_date >= now() " +
                "limit %d ",
                status.name(), Config.DB_FETCH_LIMIT);

        return findAll(query);
    }

    public static List<Link> getFailedLinks(Status status, int retryLimit) {
        final String query = String.format(
                "select *, p.price as product_price from link " +
                "inner join customer_plan as cp on cp.id = customer_plan_id " +
                "inner join product as p on p.id = product_id " +
                "where status = '%s'" +
                "  and retry < %d " +
                "  and cp.active = true " +
                "  and cp.due_date >= now() " +
                "limit %d ",
                status.name(), retryLimit, Config.DB_FETCH_LIMIT);

        return findAll(query);
    }

    public static List<Link> findActiveSellerPriceList(Long productId) {
        final String query = String.format(
                "select *, p.price as product_price from link " +
                "inner join product as p on p.id = product_id " +
                "where product_id = %d " +
                "  and status in (%s) " +
                "order by price",
                productId, Status.getJoinedPositives());

        return findAll(query);
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
     * @param link
     * @return boolean
     */
    public static boolean makeAvailable(Link link) {
        Connection con = null;
        try {
            con = DBUtils.getTransactionalConnection();

            boolean res1 = DBUtils.executeQuery(
                con,
                String.format(
                    "update link " +
                    "set name = '%s', sku = '%s', brand = '%s', seller = '%s', shipment = '%s', price = %f, " +
                        "status = '%s', previous_status = '%s', site_id = %d, website_class_name = '%s', last_update = now() " +
                    "where link_id = %d ",
                    link.getName(), link.getSku(), link.getBrand(), link.getSeller(), link.getShipment(),
                        link.getPrice(), link.getStatus().name(), link.getPreviousStatus().name(),
                        link.getSiteId(), link.getWebsiteClassName(),
                        link.getId()),

                String.format("Failed to make a link available. Link Id: %d", link.getId())
            );

            if (res1) {
                link.setStatus(link.getPreviousStatus());

                boolean res2 = changeStatus(con, new StatusChange(link, Status.AVAILABLE));
                if (res2) {
                    PriceChange change = new PriceChange(link.getId(), link.getProductId(), link.getPrice());
                    change.setLinkOnly(true);
                    boolean res3 = changePrice(con, change);
                    if (res3 && link.getSpecList() != null && link.getSpecList().size() > 0) {

                        DBUtils.executeQuery(
                            con,
                            String.format("delete from link_spec where link_id = %d ", link.getId()),
                            String.format("Failed to delete link specs. Link Id: %d", link.getId())
                        );

                        String[] queries = new String[link.getSpecList().size()];
                        for (int i = 0; i < queries.length; i++) {
                            LinkSpec spec = link.getSpecList().get(i);

                            queries[i] = String.format(
                                            "insert into link_spec " +
                                            "(link_id, _key, _value) " +
                                            "values " +
                                            "(%d, '%s', '%s');",
                                        link.getId(), spec.getKey(), spec.getValue());
                        }
                        return DBUtils.executeBatchQueries(con, queries, String.format("Failed to add spec list of a link. Link Id: %d", link.getId()));
                    }
                    DBUtils.commit(con);
                } else {
                    DBUtils.rollback(con);
                }

                return res2;
            } else {
                DBUtils.rollback(con);
            }
        } catch (SQLException e) {
            DBUtils.rollback(con);
            log.error("Failed to make available a link. Link Id: " + link.getId(), e);
        } finally {
            DBUtils.close(con);
        }

        return false;
    }

    public static boolean changeStatus(Connection con, StatusChange change) {
        boolean result = DBUtils.executeBatchQueries(
            con,
            new String[] {
                String.format(
                    "insert into link_history " +
                    "(link_id, status, http_status) " +
                    "values " +
                    "(%d, '%s', %d);",
                    change.getLink().getId(), change.getNewStatus().name(), change.getLink().getHttpStatus()),

                String.format(
                    "update link " +
                    "set status = '%s', previous_status = '%s', http_status = %d, last_update = now() " +
                    "where link_id = %d ",
                    change.getNewStatus().name(), change.getLink().getStatus().name(), change.getLink().getHttpStatus(), change.getLink().getId())
            }, String.format("Failed to change status. Link Id: %d, Old Status: %s, New Status: %s", change.getLink().getId(), change.getLink().getStatus(), change.getNewStatus())

        );

        return result;
    }

    public static boolean changePrice(Connection con, PriceChange change) {
        return DBUtils.executeBatchQueries(
            con,
            new String[] {
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
            model.setLastUpdate(rs.getDate("last_update"));
            model.setStatus(Status.valueOf(rs.getString("status")));
            model.setRetry(rs.getInt("retry"));
            model.setWebsiteClassName(rs.getString("website_class_name"));

            model.setCustomerId(rs.getLong("customer_id"));
            model.setCustomerPlanId(rs.getLong("customer_plan_id"));
            model.setSiteId(rs.getLong("site_id"));

            model.setProductId(rs.getLong("product_id"));
            model.setProductPrice(rs.getBigDecimal("product_price"));

            if (rs.getString("previous_status") != null) {
                model.setPreviousStatus(Status.valueOf(rs.getString("previous_status")));
            }

            return model;
        } catch (SQLException e) {
            log.error("Failed to set link's properties", e);
        }
        return null;
    }

}
