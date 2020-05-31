package io.inprice.scrapper.manager.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.ProductPrice;
import io.inprice.scrapper.manager.info.ProductLinks;

public class ProductRepository {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  public boolean createAProductFromLink(Connection con, Link link) throws SQLException {
    final String query = 
      "insert into product " + 
      "(code, name, brand, price, company_id) " + 
      "values (?, ?, ?, ?, ?) ";

    try (PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setString(++i, link.getSku());
      pst.setString(++i, link.getName());
      pst.setString(++i, link.getBrand());
      pst.setBigDecimal(++i, link.getPrice());
      pst.setLong(++i, link.getCompanyId());

      return (pst.executeUpdate() > 0);
    }
  }

  public boolean updatePrice(List<ProductPrice> ppList, String zeroizedIds) {
    final String q1 =
      "insert into product_price " +
      "(product_id, price, min_platform, min_seller, min_price, min_diff, avg_price, avg_diff, " +
        "max_platform, max_seller, max_price, max_diff, competitors, position, ranking, ranking_with, suggested_price, company_id) " + 
      "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    int successCounter = 0;
    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      if (ppList != null) {
        for (ProductPrice pp : ppList) {
          Long lastPriceId = null;

          try (PreparedStatement pst = con.prepareStatement(q1, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pst.setLong(++i, pp.getProductId());
            pst.setBigDecimal(++i, pp.getPrice());
            pst.setString(++i, pp.getMinPlatform());
            pst.setString(++i, pp.getMinSeller());
            pst.setBigDecimal(++i, pp.getMinPrice());
            pst.setBigDecimal(++i, pp.getMinDiff());
            pst.setBigDecimal(++i, pp.getAvgPrice());
            pst.setBigDecimal(++i, pp.getAvgDiff());
            pst.setString(++i, pp.getMaxPlatform());
            pst.setString(++i, pp.getMaxSeller());
            pst.setBigDecimal(++i, pp.getMaxPrice());
            pst.setBigDecimal(++i, pp.getMaxDiff());
            pst.setInt(++i, pp.getCompetitors());
            pst.setInt(++i, pp.getPosition());
            pst.setInt(++i, pp.getRanking());
            pst.setInt(++i, pp.getRankingWith());
            pst.setBigDecimal(++i, pp.getSuggestedPrice());
            pst.setLong(++i, pp.getCompanyId());
            if (pst.executeUpdate() > 0) {
              try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                  lastPriceId = generatedKeys.getLong(1);
                }
              }
            }
          }

          if (lastPriceId != null) {
            final String q2 =
              "update product " +
              "set price=?, last_price_id=?, updated_at=now() " +
              "where id=? " +
              "  and company_id=?";

            try (PreparedStatement pst = con.prepareStatement(q2)) {
              int i = 0;
              pst.setBigDecimal(++i, pp.getPrice());
              pst.setLong(++i, lastPriceId);
              pst.setLong(++i, pp.getProductId());
              pst.setLong(++i, pp.getCompanyId());
              if (pst.executeUpdate() > 0) {
                successCounter++;
              }
            }
          }
        }
      }

      if (zeroizedIds.length() > 1) {
        db.executeQuery(
          con, 
          "update product set last_price_id=null, updated_at=now() where id in (" + zeroizedIds + ")", 
          "Failed to zeroize some product price info"
        );
      }

      if (zeroizedIds.length() > 1 || (ppList != null && successCounter == ppList.size())) {
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to update products' prices.", e);
      successCounter = 0;
    } finally {
      if (con != null)
        db.close(con);
    }

    return (successCounter == ppList.size());
  }

  public List<ProductLinks> getProductLinks(Long productId) {
    return db.findMultiple(
      String.format(
        "select p.id as prod_id, p.price as prod_price, l.id as link_id, l.price as link_price, l.seller, p.company_id, s.domain as site_name, " +
        "dense_rank() over (order by l.price) as ranking " +
        "from product as p " +
        "inner join link as l on l.product_id = p.id " +
        "inner join site as s on s.id = l.site_id " +
        "where p.id = %d " +
        "  and p.price > 0 " +
        "  and l.price > 0" +
        "  and l.status = '%s' " +
        "order by l.price",
        productId, LinkStatus.AVAILABLE),
      this::mapProductLinks
    );
  }

  private ProductLinks mapProductLinks(ResultSet rs) {
    ProductLinks pl = new ProductLinks();
    try {
      pl.setProductId(rs.getLong("prod_id"));
      pl.setProductPrice(rs.getBigDecimal("prod_price"));
      pl.setLinkId(rs.getLong("link_id"));
      pl.setLinkPrice(rs.getBigDecimal("link_price"));
      pl.setSeller(rs.getString("seller"));
      pl.setSiteName(rs.getString("site_name"));
      pl.setRanking(rs.getInt("ranking"));
      pl.setCompanyId(rs.getLong("company_id"));

    } catch (SQLException e) {
      log.error("Failed to set product links properties", e);
    }
    return pl;
  }

}
