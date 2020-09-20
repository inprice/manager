package io.inprice.manager.repository;

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

import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.ProductCompetitor;
import io.inprice.common.meta.CompetitorStatus;
import io.inprice.common.models.Competitor;
import io.inprice.common.models.ProductPrice;

public class ProductRepository {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

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
      pst.setInt(++i, competitor.getPosition());
      pst.setLong(++i, competitor.getCompanyId());

      return (pst.executeUpdate() > 0);
    }
  }

  public ProductPrice getProductPrice(Connection con, Long productId) {
    ProductPrice result = null;

    List<ProductCompetitor> prodCompList = db.findMultiple(con,
      String.format(
        "select id, product_id, price, position, seller, company_id, s.domain as site_name, " +
        "dense_rank() over (order by price) as ranking " +
        "from competitor " +
        "inner join site as s on s.id = site_id " +
        "where product_id = %d " +
        "  and status = '%s' " +
        "  and price > 0 " +
        "order by price",
        productId, CompetitorStatus.AVAILABLE.name()),
      this::mapProductCompetitor
    );

    if (prodCompList.size() > 0) {
      BigDecimal productPrice = findPriceById(con, productId);

      if (productPrice != null) {
        ProductCompetitor pcFirst = prodCompList.get(0);
        ProductCompetitor pcLast = prodCompList.get(prodCompList.size() - 1);

        result = new ProductPrice();
        result.setProductId(pcFirst.getProductId());
        result.setPrice(productPrice);
        result.setCompetitors(prodCompList.size());
        result.setCompanyId(pcFirst.getCompanyId());
        result.setMinPlatform(pcFirst.getSiteName());
        result.setMinSeller(pcFirst.getSeller());
        result.setMinPrice(pcFirst.getPrice());
        result.setAvgPrice(productPrice);
        result.setMaxPlatform(pcLast.getSiteName());
        result.setMaxSeller(pcLast.getSeller());
        result.setMaxPrice(pcLast.getPrice());
        result.setSuggestedPrice(productPrice);

        //finding total, ranking and rankingWith
        int ranking = 0;
        int rankingWith = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (ProductCompetitor pc: prodCompList) {
          total = total.add(pc.getPrice());
          if (ranking == 0 && productPrice.compareTo(pc.getPrice()) <= 0) {
            ranking = pc.getRanking();
          }
          if (productPrice.compareTo(pc.getPrice()) == 0) {
            rankingWith++;
          }
        }
        if (ranking == 0) {
          ranking = pcLast.getRanking() + 1;
        }
        result.setRanking(ranking);
        result.setRankingWith(rankingWith);

        // finding avg price
        if (total.compareTo(BigDecimal.ZERO) > 0) {
          result.setAvgPrice(total.divide(BigDecimal.valueOf(prodCompList.size()), 2, BigDecimal.ROUND_HALF_UP));
        }

        // setting diffs
        result.setMinDiff(findDiff(result.getPrice(), pcFirst.getPrice()));
        result.setAvgDiff(findDiff(result.getPrice(), result.getAvgPrice()));
        result.setMaxDiff(findDiff(result.getPrice(), pcLast.getPrice()));

        //finding product position
        if (productPrice.compareTo(result.getMinPrice()) <= 0) {
          result.setPosition(1);
          result.setMinPlatform("Yours");
          result.setMinSeller("You");
          result.setMinPrice(productPrice);
          result.setMinDiff(BigDecimal.ZERO);
        } else if (productPrice.compareTo(result.getAvgPrice()) < 0) {
          result.setPosition(2);
        } else if (productPrice.compareTo(result.getAvgPrice()) == 0) {
          result.setPosition(3);
        } else if (productPrice.compareTo(result.getMaxPrice()) < 0) {
          result.setPosition(4);
        } else {
          result.setPosition(5);
          result.setMaxPlatform("Yours");
          result.setMaxSeller("You");
          result.setMaxPrice(productPrice);
          result.setMaxDiff(BigDecimal.ZERO);
        }
        result.setProdCompList(prodCompList);
      }
    }

    return result;
  }

  private BigDecimal findDiff(BigDecimal first, BigDecimal second) {
    BigDecimal BigDecimal_AHUNDRED = new BigDecimal(100);
    BigDecimal result = BigDecimal_AHUNDRED;
    if (first.compareTo(BigDecimal.ZERO) > 0 && second.compareTo(BigDecimal.ZERO) > 0) {
     result = second.divide(first, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE).multiply(BigDecimal_AHUNDRED).setScale(2);
    }
    return result;
  }

  public boolean updatePrice(Connection con, List<ProductPrice> ppList, String zeroizedIds) {
    boolean success = false;
    int successCounter = 0;
    try {

      if (ppList != null) {
        for (ProductPrice pp : ppList) {
          Long lastPriceId = null;

          final String q1 =
            "insert into product_price " +
            "(product_id, price, min_platform, min_seller, min_price, min_diff, avg_price, avg_diff, " +
              "max_platform, max_seller, max_price, max_diff, competitors, position, ranking, ranking_with, suggested_price, company_id) " + 
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    
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
              "set price=?, position=?, last_price_id=?, updated_at=now() " +
              "where id=? " +
              "  and company_id=?";

            try (PreparedStatement pst = con.prepareStatement(q2)) {
              int i = 0;
              pst.setBigDecimal(++i, pp.getPrice());
              pst.setLong(++i, lastPriceId);
              pst.setInt(++i, pp.getPosition());
              pst.setLong(++i, pp.getProductId());
              pst.setLong(++i, pp.getCompanyId());
              if (pst.executeUpdate() > 0) {
                successCounter++;

                //if any competitor position changed then review and update its all competitors' positions
                for (ProductCompetitor pc: pp.getProdCompList()) {
                  int newPosition = pc.getPosition();
                  if (pc.getPrice().compareTo(pp.getMinPrice()) <= 0) {
                    newPosition = 1;
                  } else if (pc.getPrice().compareTo(pp.getAvgPrice()) < 0) {
                    newPosition = 2;
                  } else if (pc.getPrice().compareTo(pp.getAvgPrice()) == 0) {
                    newPosition = 3;
                  } else if (pc.getPrice().compareTo(pp.getMaxPrice()) < 0) {
                    newPosition = 4;
                  } else {
                    newPosition = 5;
                  }
                  if (newPosition != pc.getPosition().intValue()) {
                    addCompetitorPriceChange(con,
                      pc.getId(),
                      pc.getPrice(),
                      newPosition,
                      pc.getProductId(),
                      pc.getCompanyId()
                    );
                  }
                }
              }
            }
          }
        }
      }

      boolean zeroized = false;
      if (StringUtils.isNotBlank(zeroizedIds)) {
        zeroized = db.executeQuery(
          con, 
          "update product set position=3, last_price_id=null, updated_at=now() where id in (" + zeroizedIds + ")", 
          "Failed to zeroize some product price info"
        );
      }

      if (zeroized || (ppList != null && successCounter == ppList.size())) {
        db.commit(con);
        success = true;
      } else {
        db.rollback(con);
      }

    } catch (Exception e) {
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

  private void addCompetitorPriceChange(Connection con, long competitorId, BigDecimal price, int position, long productId, long companyId) {
    final String q1 = 
      "update competitor " + 
      "set position=?, last_update=now() " + 
      "where id=?";

    try (PreparedStatement pst1 = con.prepareStatement(q1)) {
      int i = 0;
      pst1.setInt(++i, position);
      pst1.setLong(++i, competitorId);

      if (pst1.executeUpdate() > 0) {
        String q2 = "insert into competitor_price (competitor_id, price, position, product_id, company_id) values (?, ?, ?, ?, ?)";
        try (PreparedStatement pst2 = con.prepareStatement(q2)) {
          int j = 0;
          pst2.setLong(++j, competitorId);
          pst2.setBigDecimal(++i, price);
          pst2.setInt(++j, position);
          pst2.setLong(++j, productId);
          pst2.setLong(++j, companyId);
          pst2.executeUpdate();
        }
        
      }
    } catch (Exception e) {
      log.error("Failed to add a competitor price change.", e);
    }
  }

  private BigDecimal findPriceById(Connection con, Long id) {
    try (PreparedStatement pst = con.prepareStatement("select price from product where id="+id); 
      ResultSet rs = pst.executeQuery()) {
      if (rs.next()) {
        return rs.getBigDecimal("price");
      }
    } catch (Exception e) {
      log.error("Failed to find product price by id.", e);
    }
    return null;
  }

  private ProductCompetitor mapProductCompetitor(ResultSet rs) {
    ProductCompetitor pc = new ProductCompetitor();
    try {
      pc.setId(rs.getLong("id"));
      pc.setProductId(rs.getLong("product_id"));
      pc.setPrice(rs.getBigDecimal("price"));
      pc.setPosition(rs.getInt("position"));
      pc.setSeller(rs.getString("seller"));
      pc.setSiteName(rs.getString("site_name"));
      pc.setRanking(rs.getInt("ranking"));
      pc.setCompanyId(rs.getLong("company_id"));

    } catch (SQLException e) {
      log.error("Failed to set product competitors properties", e);
    }
    return pc;
  }

}
