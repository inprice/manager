package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.common.helpers.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SiteRepository {

  private static final Logger log = LoggerFactory.getLogger(SiteRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  public List<Site> getAll() {
    return db.findMultiple("select * from site where active = true", this::map);
  }

  private Site map(ResultSet rs) {
    try {
      Site model = new Site();
      model.setId(rs.getLong("id"));
      model.setActive(rs.getBoolean("active"));
      model.setName(rs.getString("name"));
      model.setDomain(rs.getString("domain"));
      model.setClassName(rs.getString("class_name"));
      model.setCreatedAt(rs.getDate("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set site's properties", e);
    }
    return null;
  }
}
