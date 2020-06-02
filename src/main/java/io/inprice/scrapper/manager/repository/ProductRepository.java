package io.inprice.scrapper.manager.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.models.Competitor;
import io.inprice.scrapper.common.models.ProductPrice;
import io.inprice.scrapper.manager.info.ProductCompetitors;

public class ProductRepository {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  private static final BigDecimal BigDecimal_AHUNDRED = new BigDecimal(100);

  public boolean createAProductFromCompetitor(Connection con, Competitor competitor) throws SQLException {
    final String query = 
      "insert into product " + 
      "(code, name, brand, price, company_id) " + 
      "values (?, ?, ?, ?, ?) ";

    try (PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setString(++i, competitor.getSku());
      pst.setString(++i, competitor.getName());
      pst.setString(++i, competitor.getBrand());
      pst.setBigDecimal(++i, competitor.getPrice());
      pst.setLong(++i, competitor.getCompanyId());

      return (pst.executeUpdate() > 0);
    }
  }

  public ProductPrice getProductCompetitors(Connection con, Long productId) {
    ProductPrice result = null;

    List<ProductCompetitors> prodCompetitors = db.findMultiple(con,
      String.format(
        "select p.id as prod_id, p.price as prod_price, l.id as competitor_id, l.price as competitor_price, l.seller, p.company_id, s.domain as site_name, " +
        "dense_rank() over (order by l.price) as ranking " +
        "from product as p " +
        "inner join competitor as l on l.product_id = p.id " +
        "inner join site as s on s.id = l.site_id " +
        "where p.id = %d " +
        "  and p.price > 0 " +
        "  and l.price > 0 " +
        "order by l.price",
        productId),
      this::mapProductCompetitors
    );

    if (prodCompetitors.size() > 0) {
      ProductCompetitors plFirst = prodCompetitors.get(0);
      ProductCompetitors plLast = prodCompetitors.get(prodCompetitors.size() - 1);

      result = new ProductPrice();
      result.setProductId(plFirst.getProductId());
      result.setPrice(plFirst.getProductPrice());
      result.setCompetitors(prodCompetitors.size());
      result.setCompanyId(plFirst.getCompanyId());
      result.setMinPlatform(plFirst.getSiteName());
      result.setMinSeller(plFirst.getSeller());
      result.setMinPrice(plFirst.getCompetitorPrice());
      result.setAvgPrice(plFirst.getProductPrice());
      result.setMaxPlatform(plLast.getSiteName());
      result.setMaxSeller(plLast.getSeller());
      result.setMaxPrice(plLast.getCompetitorPrice());
      result.setSuggestedPrice(plFirst.getProductPrice());

      //finding total, ranking and rankingWith
      int ranking = 0;
      int rankingWith = 0;
      BigDecimal total = BigDecimal.ZERO;
      for (ProductCompetitors pl : prodCompetitors) {
        total = total.add(pl.getCompetitorPrice());
        if (pl.getProductPrice().compareTo(pl.getCompetitorPrice()) <= 0) {
          ranking = pl.getRanking();
        }
        if (pl.getProductPrice().compareTo(pl.getCompetitorPrice()) == 0) {
          rankingWith++;
        }
      }
      if (ranking == 0) {
        ranking = plLast.getRanking() + 1;
      }
      result.setRanking(ranking);
      result.setRankingWith(rankingWith);

      // finding avg price
      if (total.compareTo(BigDecimal.ZERO) > 0) {
        result.setAvgPrice(total.divide(BigDecimal.valueOf(prodCompetitors.size()), 2, BigDecimal.ROUND_HALF_UP));
      }

      // setting diffs
      result.setMinDiff(findDiff(result.getPrice(), plFirst.getCompetitorPrice()));
      result.setAvgDiff(findDiff(result.getPrice(), result.getAvgPrice()));
      result.setMaxDiff(findDiff(result.getPrice(), plLast.getCompetitorPrice()));

      //finding position
      result.setPosition(3);// average
      BigDecimal basePrice = plFirst.getProductPrice();

      if (basePrice.compareTo(result.getMinPrice()) <= 0) {
        result.setPosition(1);
        result.setMinPlatform("Yours");
        result.setMinSeller("You");
        result.setMinPrice(plFirst.getProductPrice());
        result.setMinDiff(BigDecimal.ZERO);
      } else if (basePrice.compareTo(result.getAvgPrice()) < 0) {
        result.setPosition(2);
      } else if (basePrice.compareTo(result.getMaxPrice()) < 0) {
        result.setPosition(4);
      } else {
        result.setPosition(5);
        result.setMaxPlatform("Yours");
        result.setMaxSeller("You");
        result.setMaxPrice(plLast.getProductPrice());
        result.setMaxDiff(BigDecimal.ZERO);
      }
    }

    return result;
  }

  private BigDecimal findDiff(BigDecimal first, BigDecimal second) {
    BigDecimal result = BigDecimal_AHUNDRED;
    if (first.compareTo(BigDecimal.ZERO) > 0 && second.compareTo(BigDecimal.ZERO) > 0) {
     result = second.divide(first, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE).multiply(BigDecimal_AHUNDRED).setScale(2);
    }
    return result;
  }

  public boolean updatePrice(Connection con, List<ProductPrice> ppList, String zeroizedIds) {
    final String q1 =
      "insert into product_price " +
      "(product_id, price, min_platform, min_seller, min_price, min_diff, avg_price, avg_diff, " +
        "max_platform, max_seller, max_price, max_diff, competitors, position, ranking, ranking_with, suggested_price, company_id) " + 
      "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    boolean success = false;
    int successCounter = 0;
    try {

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

      boolean zeroized = false;
      if (StringUtils.isNotBlank(zeroizedIds)) {
        zeroized = db.executeQuery(
          con, 
          "update product set last_price_id=null, updated_at=now() where id in (" + zeroizedIds + ")", 
          "Failed to zeroize some product price info"
        );
      }

      if (zeroized || (ppList != null && successCounter == ppList.size())) {
        db.commit(con);
        success = true;
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null)
        db.rollback(con);
      success = false;
      log.error("Failed to update products' prices.", e);
    } finally {
      if (con != null)
        db.close(con);
    }

    return success;
  }

  private ProductCompetitors mapProductCompetitors(ResultSet rs) {
    ProductCompetitors pl = new ProductCompetitors();
    try {
      pl.setProductId(rs.getLong("prod_id"));
      pl.setProductPrice(rs.getBigDecimal("prod_price"));
      pl.setCompetitorId(rs.getLong("competitor_id"));
      pl.setCompetitorPrice(rs.getBigDecimal("competitor_price"));
      pl.setSeller(rs.getString("seller"));
      pl.setSiteName(rs.getString("site_name"));
      pl.setRanking(rs.getInt("ranking"));
      pl.setCompanyId(rs.getLong("company_id"));

    } catch (SQLException e) {
      log.error("Failed to set product competitors properties", e);
    }
    return pl;
  }

}
