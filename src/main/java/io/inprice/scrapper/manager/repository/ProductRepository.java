package io.inprice.scrapper.manager.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.info.PriceUpdate;
import io.inprice.scrapper.manager.info.ProductLinks;

public class ProductRepository {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  public boolean createAProductFromLink(Connection con, Link link) throws SQLException {
    final String query = 
      "insert into product " + 
      "(code, name, brand, price, company_id) " + 
      "values (?, ?, ?, ?, ?) ";

    try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      int i = 0;
      pst.setString(++i, link.getSku());
      pst.setString(++i, link.getName());
      pst.setString(++i, link.getBrand());
      pst.setBigDecimal(++i, link.getPrice());
      pst.setLong(++i, link.getCompanyId());

      if (pst.executeUpdate() > 0) {
        try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            link.setProductId(generatedKeys.getLong(1));
            return addAPriceHistory(con, link);
          }
        }
      }
    }

    return false;
  }

  private boolean addAPriceHistory(Connection con, Link link) {
    final String query = 
      "insert into product_price " + 
      "(product_id, price, company_id) " + 
      "values (?, ?, ?) ";

    try (PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setLong(++i, link.getProductId());
      pst.setBigDecimal(++i, link.getPrice());
      pst.setLong(++i, link.getCompanyId());

      return (pst.executeUpdate() > 0);
    } catch (Exception e) {
      log.error("Error", e);
    }

    return false;
  }

  public boolean updatePrice(PriceUpdate pu) {
    List<String> queries = new ArrayList<>(2);
    queries.add(
      String.format(
        "insert into product_price " +
        "(product_id, price, position, min_platform, min_seller, min_price, avg_price, " +
          "max_platform, max_seller, max_price, links_count, company_id) " + 
        "values (%d, %f, %d, '%s', '%s', %f, %f, '%s', '%s', %f, %d, %d);",
        pu.getProductId(), pu.getBasePrice(), pu.getPosition(), pu.getMinPlatform(), pu.getMinSeller(), pu.getMinPrice(), pu.getAvgPrice(),
        pu.getMaxPlatform(), pu.getMaxSeller(), pu.getMaxPrice(), pu.getLinksCount(), pu.getCompanyId())
    );

    queries.add(
      String.format(
        "update product " +
        "set position=%d, min_platform='%s', min_seller='%s', min_price=%f, avg_price=%f, " +
           "max_platform='%s', max_seller='%s', max_price=%f, links_count=%d, updated_at=now() " +
        "where id=%d " +
        "  and company_id=%d",
        pu.getPosition(), pu.getMinPlatform(), pu.getMinSeller(), pu.getMinPrice(), pu.getAvgPrice(),
        pu.getMaxPlatform(), pu.getMaxSeller(), pu.getMaxPrice(), pu.getLinksCount(), pu.getProductId(), pu.getCompanyId())
    );

    return db.executeBatchQueries(
        queries, 
        "Failed to update product price. " + pu.toString()
    );
  }

  public List<ProductLinks> getProductLinks(Long productId) {
    return db.findMultiple(
      String.format(
        "select p.id as prod_id, p.price as prod_price, l.id as link_id, l.price as link_price, l.seller, p.company_id, s.domain as site_name " +
        "from product as p " +
        "inner join link as l on l.product_id = p.id " +
        "inner join site as s on s.id = l.site_id " +
        "where p.id = %d " +
        "  and p.price > 0 " +
        "  and l.price > 0" +
        "  and l.status != '%s' " +
        "order by l.price ",
        productId, LinkStatus.PAUSED),
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
      pl.setSeller(rs.getString("seller"));
      pl.setCompanyId(rs.getLong("company_id"));

    } catch (SQLException e) {
      log.error("Failed to set product's properties", e);
    }
    return pl;
  }

}
