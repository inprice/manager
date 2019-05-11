package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.Sector;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Sectors {

    private static final Logger log = new Logger(Sectors.class);

    private static final String PLAIN_SEARCH_QUERY = "select * from sector ";

    public static List<Sector> getAll() {
        return findAll(PLAIN_SEARCH_QUERY);
    }

    public static Sector getOne(Long id) {
        List<Sector> list = findAll(String.format("%s where id = %d", PLAIN_SEARCH_QUERY, id));
        if (list != null && list.size() > 0)
            return list.get(0);
        else
            return null;
    }

    private static List<Sector> findAll(String query) {
        List<Sector> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Error in fetching Sectors", e);
        }

        return result;
    }

    private static Sector map(ResultSet rs) throws SQLException {
        Sector sector = new Sector();
        sector.setId(rs.getLong("id"));
        sector.setName(rs.getString("name"));

        return sector;
    }

}
