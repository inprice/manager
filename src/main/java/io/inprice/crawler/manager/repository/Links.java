package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.config.Config;
import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.meta.CrawlingStatus;
import io.inprice.crawler.common.meta.LinkStatus;
import io.inprice.crawler.common.models.Link;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Links {

    private static final Logger log = new Logger(Links.class);

    private static final String PLAIN_SEARCH_QUERY = "select * from link ";

    public static boolean setCrawlingStatuses(CrawlingStatus crawlingStatus, String forTheseIds) {
        final String query =
                String.format("update link " +
                        "set crawling_status = '%s', crawling_status_date = now() " +
                        "where id in (%s) " +
                        "  and crawling_status != '%s'", crawlingStatus.name(), forTheseIds, crawlingStatus.name());

        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            log.error("Error in updating Links to change their crawling status", e);
        }

        return false;
    }

    public static List<Link> getAll(Long productId) {
        return findAll(String.format("%s where product_id = %d", PLAIN_SEARCH_QUERY, productId));
    }

    public static Link getOne(Long id) {
        List<Link> list = findAll(String.format("%s where id = %d", PLAIN_SEARCH_QUERY, id));
        if (list != null && list.size() > 0)
            return list.get(0);
        else
            return null;
    }

    public static List<Link> getActiveSites() {
        String query = String.format("%s " +
                "inner join customer_plan as cp cp.id = customer_plan_id " +
                "where status = '%s'" +
                    "   and crawling_status = '%s'   " +
                    "   and cp.active = true " +
                    "   and cp.due_date >= now() " +
                "limit %d ",
                PLAIN_SEARCH_QUERY, LinkStatus.ACTIVE, CrawlingStatus.WAITING, Config.DB_FETCH_LIMIT);

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
            log.error("Error in fetching Links", e);
        }

        return result;
    }

    private static Link map(ResultSet rs) throws SQLException {
        Link link = new Link();
        link.setId(rs.getLong("id"));
        link.setTitle(rs.getString("title"));
        link.setCode(rs.getString("code"));
        link.setUrl(rs.getString("url"));
        link.setAltUrl(rs.getString("alt_url"));
        link.setBrand(rs.getString("brand"));
        link.setSeller(rs.getString("seller"));
        link.setShipment(rs.getString("shipment"));
        link.setPrice(rs.getBigDecimal("price"));
        link.setPriceDate(rs.getDate("price_date"));
        link.setCrawlingStatusDate(rs.getDate("crawling_status_date"));
        link.setNote(rs.getString("note"));

        if (rs.getString("status") != null) {
            link.setStatus(LinkStatus.valueOf(rs.getString("status")));
        }

        if (rs.getString("crawling_status") != null) {
            link.setCrawlingStatus(CrawlingStatus.valueOf(rs.getString("crawling_status")));
        }

        link.setCustomerPlanId(rs.getLong("customer_plan_id"));
        link.setProductId(rs.getLong("product_id"));
        link.setSiteId(rs.getLong("site_id"));

        return link;
    }

}
