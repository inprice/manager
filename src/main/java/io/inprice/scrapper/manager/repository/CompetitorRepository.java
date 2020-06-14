package io.inprice.scrapper.manager.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.helpers.RepositoryHelper;
import io.inprice.scrapper.common.info.PriceUpdateInfo;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.common.meta.PlanStatus;
import io.inprice.scrapper.common.models.Competitor;
import io.inprice.scrapper.common.models.CompetitorHistory;
import io.inprice.scrapper.common.models.CompetitorSpec;
import io.inprice.scrapper.manager.config.Props;

public class CompetitorRepository {

  private static final Logger log = LoggerFactory.getLogger(CompetitorRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  /**
   * This method can be used for both competitors and imported products at the same
   * time.
   *
   */
  public List<Competitor> getCompetitors(CompetitorStatus status) {
    final String query = String.format(
        "select l.*, p.price as product_price from competitor as l " + 
        "inner join company as c on c.id = l.company_id " +
        "left join product as p on p.id = l.product_id " + 
        "where l.status = '%s' " + 
        "  and c.plan_status = '%s' " + 
        "  and c.due_date >= now() " + 
        "  and (l.last_check is null or l.last_check < now() - interval 30 minute) " + 
        "limit %d",
        status.name(), PlanStatus.ACTIVE.name(), Props.DB_FETCH_LIMIT());

    return findAll(query);
  }

  /**
   * This method can be used for both competitors and imported products at the same
   * time.
   *
   */
  public List<Competitor> getFailedCompetitors(CompetitorStatus status, int retryLimit) {
    final String query = String.format(
        "select l.*, p.price as product_price from competitor as l " +
        "inner join company as c on c.id = l.company_id " +
        "left join product as p on p.id = l.product_id " +
        "where l.status = '%s' " + 
        "  and l.retry < %d " + 
        "  and c.plan_status = '%s' " + 
        "  and c.due_date >= now() " + 
        "  and (l.last_check is null or l.last_check < now() - interval 30 minute) " + 
        "limit %d",
        status.name(), retryLimit, PlanStatus.ACTIVE.name(), Props.DB_FETCH_LIMIT());

    return findAll(query);
  }

  public void setLastCheckTime(String ids, boolean increaseRetry) {
    final String query = 
        "update competitor " + 
        "set last_check=now() " + (increaseRetry ? ", retry=retry+1 " : "") +
        "where id in (" + ids + ") ";

    db.executeQuery(query, "Failed to set last check time of ids: " + ids);
  }

  /**
   * A competitor which is in NEW status becomes AVAILABLE with the help of this method.
   * This method does several database operations, please see below;
   *
   * - All the basic information of the competitor is set first 
   * - In order to add a status change into competitor_history table, changeStatus method is called 
   * - and also changePrice method is called for adding a price change row into competitor_price table
   * - lastly, specs of the competitor are added
   *
   * @return boolean
   */
  public boolean makeAvailable(Competitor competitor) {
    boolean result = false;

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      final String q1 = 
        "update competitor " + 
        "set name=?, sku=?, brand=?, seller=?, shipment=?, price=?, pre_status=status, status=?, " +
        "site_id=?, website_class_name=?, last_update=now(), retry=0, http_status=0 " +
        "where id = ? " + 
        "  and status != ?";

      try (PreparedStatement pst = con.prepareStatement(q1)) {
        int i = 0;
        pst.setString(++i, competitor.getName());
        pst.setString(++i, competitor.getSku());
        pst.setString(++i, competitor.getBrand());
        pst.setString(++i, competitor.getSeller());
        pst.setString(++i, competitor.getShipment());
        pst.setBigDecimal(++i, competitor.getPrice());
        pst.setString(++i, competitor.getStatus().name());
        pst.setLong(++i, competitor.getSiteId());
        pst.setString(++i, competitor.getWebsiteClassName());
        pst.setLong(++i, competitor.getId());
        pst.setString(++i, competitor.getStatus().name());

        result = (pst.executeUpdate() > 0);
      } catch (Exception e) {
        log.error("Failed to make a competitor available. competitor Id: " + competitor.getId(), e);
      }

      if (result && competitor.getProductId() != null) {

        addStatusChangeHistory(con, competitor);
        addPriceChangeHistory(con, competitor);

        if (competitor.getSpecList() != null && competitor.getSpecList().size() > 0) {
          // deleting old specs if any
          executeSimpleQuery(con, "delete from competitor_spec where competitor_id=" + competitor.getId());

          int j;
          final String q3 = "insert into competitor_spec (competitor_id, _key, _value, product_id, company_id) values (?, ?, ?, ?, ?)";
          try (PreparedStatement pst = con.prepareStatement(q3)) {
            for (int i = 0; i < competitor.getSpecList().size(); i++) {
              CompetitorSpec spec = competitor.getSpecList().get(i);

              j = 0;
              pst.setLong(++j, competitor.getId());
              pst.setString(++j, spec.getKey());
              pst.setString(++j, spec.getValue());
              pst.setLong(++j, competitor.getProductId());
              pst.setLong(++j, competitor.getCompanyId());
              pst.addBatch();
            }
            pst.executeBatch();
          }
        }
      } else {
        log.warn("competitor is already in {} status. competitor Id: {} ", competitor.getStatus().name(), competitor.getId());
      }

      if (result) {
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to make available a competitor. competitor Id: " + competitor.getId(), e);
    } finally {
      if (con != null)
        db.close(con);
    }

    return result;
  }

  public boolean changeStatus(StatusChange change) {
    boolean result = false;

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      final String oldStatusName = change.getOldStatus().name();
      String newStatusName = change.getCompetitor().getStatus().name();

      if (change.getCompetitor().getHttpStatus() == null)
        change.getCompetitor().setHttpStatus(0);

      if (CompetitorStatus.RESUMED.equals(change.getCompetitor().getStatus())) {
        CompetitorStatus originalStatus = findThirdBackStatusForResumedCompetitors(con, change.getCompetitor().getId());
        if (originalStatus != null) {
          newStatusName = originalStatus.name();
        }
      }

      final String q1 = 
        "update competitor " + 
        "set status=?, pre_status=?, http_status=?, last_update=now() " +
        (change.getCompetitor().getHttpStatus() != 0 ? ", retry=retry+1 " : "") + 
        "where id=? " + 
        "  and status!=?";

      try (PreparedStatement pst = con.prepareStatement(q1)) {
        int i = 0;
        pst.setString(++i, newStatusName);
        pst.setString(++i, oldStatusName);
        pst.setInt(++i, change.getCompetitor().getHttpStatus());
        pst.setLong(++i, change.getCompetitor().getId());
        pst.setString(++i, newStatusName);

        result = (pst.executeUpdate() > 0);
      }

      if (change.getCompetitor().getProductId() != null) {
        if (result) {
          addStatusChangeHistory(con, change.getCompetitor());
        } else {
          log.warn("competitor's status is already changed! competitor Id: {}, Old Status: {}, New Status: {}",
              change.getCompetitor().getId(), oldStatusName, newStatusName);
        }
      }

      if (result) {
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to add a new status. competitor Id: " + change.getCompetitor().getId(), e);
    } finally {
      if (con != null)
        db.close(con);
    }

    return result;
  }

  public boolean changePrice(PriceUpdateInfo change) {
    boolean result = false;

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      final String q1 = 
        "update competitor " + 
        "set price=?, last_update=now() " + 
        "where id=? " + 
        "  and price<>?";

      try (PreparedStatement pst = con.prepareStatement(q1)) {
        int i = 0;
        pst.setBigDecimal(++i, change.getNewPrice());
        pst.setLong(++i, change.getCompetitorId());
        pst.setBigDecimal(++i, change.getNewPrice());
        result = (pst.executeUpdate() > 0);
      } catch (Exception e) {
        log.error("Failed to change price of a competitor at step 1. competitor Id: " + change.getCompetitorId(), e);
      }

      if (result) {
        addPriceChangeHistory(con, change);
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      db.rollback(con);
      log.error("Failed to change price. competitor Id: {}, Price: {}", change.getCompetitorId(), change.getNewPrice(), e);
    } finally {
      if (con != null)
        db.close(con);
    }

    return result;
  }

  private void addStatusChangeHistory(Connection con, Competitor competitor) {
    executeSimpleQuery(con,
      String.format(
        "insert into competitor_history (competitor_id, status, http_status, product_id, company_id) " +
        "values (%d, '%s', %d, %d, %d)",
        competitor.getId(), competitor.getStatus(), competitor.getHttpStatus(), competitor.getProductId(), competitor.getCompanyId()));
  }

  /**
   * Since the previous status of a resumed competitor must be PAUSED (thus, nonsens to
   * return back to PAUSED from RESUMED) We need to find the third status in back
   * which means we find the status before PAUSED
   *
   * 1- competitor is in any status 2- passes in PAUSED 3- passes in RESUMED 4- we must
   * return back to the status in first step
   */
  private CompetitorStatus findThirdBackStatusForResumedCompetitors(Connection con, Long competitorId) {
    final String query = String.format(
        "select * from competitor_history " + 
        "where competitor_id = %d " + 
        "order by created_at desc " + 
        "limit 3 ", competitorId);

    List<CompetitorHistory> historyList = db.findMultiple(con, query, this::historyMap);
    if (historyList != null && historyList.size() > 2) {
      return historyList.get(2).getStatus();
    }

    return null;
  }

  private void addPriceChangeHistory(Connection con, Competitor competitor) {
    addPriceChangeHistory(con, competitor.getId(), competitor.getPrice(), competitor.getProductId(), competitor.getCompanyId());
  }

  private void addPriceChangeHistory(Connection con, PriceUpdateInfo priceInfo) {
    addPriceChangeHistory(con, priceInfo.getCompetitorId(), priceInfo.getNewPrice(), priceInfo.getProductId(), priceInfo.getCompanyId());
  }

  private void addPriceChangeHistory(Connection con, long competitorId, BigDecimal price, long productId, long companyId) {
    executeSimpleQuery(con, String.format(
        "insert into competitor_price (competitor_id, price, product_id, company_id) values (%d, %f, %d, %d)",
        competitorId, price, productId, companyId));
  }

  private void executeSimpleQuery(Connection con, String query) {
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.executeUpdate();
    } catch (Exception e) {
      log.error("Failed to execute query: " + query, e);
    }
  }

  private List<Competitor> findAll(String query) {
    return db.findMultiple(query, this::map);
  }

  private Competitor map(ResultSet rs) {
    try {
      Competitor model = new Competitor(rs.getString("url"));
      model.setId(rs.getLong("id"));
      model.setName(rs.getString("name"));
      model.setSku(rs.getString("sku"));
      model.setBrand(rs.getString("brand"));
      model.setSeller(rs.getString("seller"));
      model.setShipment(rs.getString("shipment"));
      model.setPrice(rs.getBigDecimal("price"));
      model.setLastCheck(rs.getTimestamp("last_check"));
      model.setLastUpdate(rs.getTimestamp("last_update"));
      model.setStatus(CompetitorStatus.valueOf(rs.getString("status")));
      model.setHttpStatus(rs.getInt("http_status"));
      model.setPreStatus(CompetitorStatus.valueOf(rs.getString("pre_status")));
      model.setRetry(rs.getInt("retry"));
      model.setWebsiteClassName(rs.getString("website_class_name"));
      model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
      model.setProductId(RepositoryHelper.nullLongHandler(rs, "product_id"));
      model.setSiteId(RepositoryHelper.nullLongHandler(rs, "site_id"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      model.setProductPrice(rs.getBigDecimal("product_price"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set competitor's properties", e);
    }
    return null;
  }

  private CompetitorHistory historyMap(ResultSet rs) {
    try {
      CompetitorHistory model = new CompetitorHistory();
      model.setId(rs.getLong("id"));
      model.setCompetitorId(rs.getLong("competitor_id"));
      model.setStatus(CompetitorStatus.valueOf(rs.getString("status")));
      model.setHttpStatus(rs.getInt("http_status"));
      model.setCompanyId(rs.getLong("company_id"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set competitor's history properties", e);
    }
    return null;
  }

}
