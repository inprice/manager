package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.meta.LinkStatus;
import io.inprice.crawler.common.models.LinkPrice;
import io.inprice.crawler.common.models.Sector;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LinkPrices {

    private static final Logger log = new Logger(LinkPrices.class);

    public static List<LinkPrice> getAll(Long linkId) {
        final String query = String.format("select * from link_price where link_id = %d", linkId);

        List<LinkPrice> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                LinkPrice linkPrice = new LinkPrice();
                linkPrice.setId(rs.getLong("id"));
                linkPrice.setPrice(rs.getBigDecimal("price"));
                linkPrice.setStockStatus(rs.getString("stock_status"));
                linkPrice.setInsertAt(rs.getDate("insert_at"));

                result.add(linkPrice);
            }
        } catch (Exception e) {
            log.error("Error in fetching LinkPrices", e);
        }

        return result;
    }

    public static boolean add(Long linkId, BigDecimal price, String stockStatus) {
        final String query =
                String.format("insert into link_price (link_id, price, stock_status) " +
                        "values (%d, %f, %s) ",
                        linkId, price, stockStatus);

        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            log.error("Error in adding a new LinkPrice", e);
        }

        return false;
    }

}
