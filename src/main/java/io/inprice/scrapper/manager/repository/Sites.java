package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.manager.helpers.DBUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Sites {

    private static final Logger log = new Logger(Sites.class);

    public static List<Site> getAll() {
        return DBUtils.findMultiple("select * from site where active = true", Sites::map);
    }

    private static Site map(ResultSet rs) {
        try {
            Site model = new Site();
            model.setId(rs.getLong("id"));
            model.setActive(rs.getBoolean("active"));
            model.setName(rs.getString("name"));
            model.setDomain(rs.getString("domain"));
            model.setClassName(rs.getString("class_name"));
            model.setInsertAt(rs.getDate("insert_at"));

            model.setCountryId(rs.getLong("country_id"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set site's properties", e);
        }
        return null;
    }
}
