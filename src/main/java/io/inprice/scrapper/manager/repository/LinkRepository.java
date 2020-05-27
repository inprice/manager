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
import io.inprice.scrapper.common.info.PriceUpdateInfo;
import io.inprice.scrapper.common.info.StatusChange;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.meta.PlanStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.LinkHistory;
import io.inprice.scrapper.common.models.LinkSpec;
import io.inprice.scrapper.manager.config.Props;

public class LinkRepository {

  private static final Logger log = LoggerFactory.getLogger(LinkRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  /**
   * This method can be used for both links and imported products at the same
   * time. isLookingForImportedProducts parameter is used to distinguish the
   * searching direction between links and imported product rows
   *
   */
  public List<Link> getLinks(LinkStatus status) {
    final String query = String.format(
        "select l.*, p.price as product_price from link as l " + 
        "inner join product as p on p.id = l.product_id " + 
        "inner join company as c on c.id = l.company_id " +
        "where l.status = '%s' " + 
        "  and c.plan_status = '%s' " + 
        "  and c.due_date >= now() " + 
        "  and l.last_check < now() - interval 30 minute " + 
        "limit %d",
        status.name(), PlanStatus.ACTIVE.name(), Props.DB_FETCH_LIMIT());

    return findAll(query);
  }

  /**
   * This method can be used for both links and imported products at the same
   * time. isLookingForImportedProducts parameter is used to distinguish the
   * searching direction between links and imported product rows
   *
   */
  public List<Link> getFailedLinks(LinkStatus status, int retryLimit) {
    final String query = String.format(
        "select l.*, p.price as product_price from link as l " +
        "inner join product as p on p.id = l.product_id " +
        "inner join company as c on c.id = l.company_id " +
        "where l.status = '%s' " + 
        "  and l.retry < %d " + 
        "  and c.plan_status = '%s' " + 
        "  and c.due_date >= now() " + 
        "  and l.last_check < now() - interval 30 minute " + 
        "limit %d",
        status.name(), retryLimit, PlanStatus.ACTIVE.name(), Props.DB_FETCH_LIMIT());

    return findAll(query);
  }

  public void setLastCheckTime(String ids, boolean increaseRetry) {
    final String query = 
        "update link " + 
        "set last_check=now() " + (increaseRetry ? ", retry=retry+1 " : "") +
        "where id in (" + ids + ") ";

    db.executeQuery(query, "Failed to set last check time of ids: " + ids);
  }

  /**
   * A link which is in NEW status becomes AVAILABLE with the help of this method.
   * This method does several database operations, please see below;
   *
   * - All the basic information of the link is set first 
   * - In order to add a status change into link_history table, changeStatus method is called 
   * - In order to add a price change into link_price table, changePrice method is called 
   * - Specs of the link are added
   *
   * @return boolean
   */
  public boolean makeAvailable(Link link) {
    boolean result = false;

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      final String q1 = 
        "update link " + 
        "set name=?, sku=?, brand=?, seller=?, shipment=?, price=?, status=?, " +
        "pre_status=?, site_id=?, website_class_name=?, last_update=now(), retry=0, http_status=0 " +
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
          // deleting old specs if any
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

      if (result) {
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to make available a link. Link Id: " + link.getId(), e);
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
      String newStatusName = change.getLink().getStatus().name();

      if (change.getLink().getHttpStatus() == null)
        change.getLink().setHttpStatus(0);

      if (LinkStatus.RESUMED.equals(change.getLink().getStatus())) {
        LinkStatus originalStatus = findThirdBackStatusForResumedLinks(con, change.getLink().getId());
        if (originalStatus != null) {
          newStatusName = originalStatus.name();
        }
      }

      final String q1 = 
        "update link " + 
        "set status=?, pre_status=?, http_status=?, last_update=now() " +
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
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to add a new status. Link Id: " + change.getLink().getId(), e);
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
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      db.rollback(con);
      log.error("Failed to change price. Link Id: {}, Price: {}", change.getLinkId(), change.getNewPrice(), e);
    } finally {
      if (con != null)
        db.close(con);
    }

    return result;
  }

  private void addStatusChangeHistory(Connection con, Link link) {
    executeSimpleQuery(con,
      String.format(
        "insert into link_history (link_id, status, http_status, company_id, product_id) " +
        "values (%d, '%s', %d, %d, %d)",
        link.getId(), link.getStatus(), link.getHttpStatus(), link.getCompanyId(), link.getProductId()));
  }

  /**
   * Since the previous status of a resumed link must be PAUSED (thus, nonsens to
   * return back to PAUSED from RESUMED) We need to find the third status in back
   * which means we find the status before PAUSED
   *
   * 1- Link is in any status 2- passes in PAUSED 3- passes in RESUMED 4- we must
   * return back to the status in first step
   */
  private LinkStatus findThirdBackStatusForResumedLinks(Connection con, Long linkId) {
    final String query = String.format(
        "select * from link_history " + 
        "where link_id = %d " + 
        "order by created_at desc " + 
        "limit 3 ", linkId);

    List<LinkHistory> historyList = db.findMultiple(con, query, this::historyMap);
    if (historyList != null && historyList.size() > 2) {
      return historyList.get(2).getStatus();
    }

    return null;
  }

  private void addPriceChangeHistory(Connection con, Link link) {
    addPriceChangeHistory(con, link.getId(), link.getPrice(), link.getCompanyId(), link.getProductId());
  }

  private void addPriceChangeHistory(Connection con, PriceUpdateInfo priceInfo) {
    addPriceChangeHistory(con, priceInfo.getLinkId(), priceInfo.getNewPrice(), priceInfo.getCompanyId(), priceInfo.getProductId());
  }

  private void addPriceChangeHistory(Connection con, long linkId, BigDecimal price, long companyId, long productId) {
    executeSimpleQuery(con, String.format(
        "insert into link_price (link_id, price, companyId, productId) values (%d, %f, %d, %d)",
        linkId, price, companyId, productId));
  }

  private void executeSimpleQuery(Connection con, String query) {
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.executeUpdate();
    } catch (Exception e) {
      log.error("Failed to execute query: " + query, e);
    }
  }

  private List<Link> findAll(String query) {
    return db.findMultiple(query, this::map);
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
      model.setStatus(LinkStatus.valueOf(rs.getString("status")));
      model.setHttpStatus(rs.getInt("http_status"));
      model.setPreStatus(LinkStatus.valueOf(rs.getString("pre_status")));
      model.setRetry(rs.getInt("retry"));
      model.setWebsiteClassName(rs.getString("website_class_name"));
      model.setCompanyId(rs.getLong("company_id"));
      model.setProductId(rs.getLong("product_id"));
      model.setSiteId(rs.getLong("site_id"));

      model.setProductPrice(rs.getBigDecimal("product_price"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set link's properties", e);
    }
    return null;
  }

  private LinkHistory historyMap(ResultSet rs) {
    try {
      LinkHistory model = new LinkHistory();
      model.setId(rs.getLong("id"));
      model.setLinkId(rs.getLong("link_id"));
      model.setStatus(LinkStatus.valueOf(rs.getString("status")));
      model.setHttpStatus(rs.getInt("http_status"));
      model.setCompanyId(rs.getLong("company_id"));
      model.setCreatedAt(rs.getDate("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set link's history properties", e);
    }
    return null;
  }

}
