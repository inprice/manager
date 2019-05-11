package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.Country;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Countries {

    private static final Logger log = new Logger(Countries.class);

    public static List<Country> getAll() {
        return findAll("select * from country");
    }

    public static List<Country> search(String term) {
        return findAll("select * from country where name like '%"+term+"%'");
    }

    public static Country getOne(long id) {
        final String query = String.format("select * from country where id = %d", id);

        Country result = null;
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                result = map(rs);
            }
        } catch (Exception e) {
            log.error("Error in getting Country by id", e);
        }

        return result;
    }

    private static List<Country> findAll(String query) {
        List<Country> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Error in fetching Countries", e);
        }

        return result;
    }

    private static Country map(ResultSet rs) throws SQLException {
        return new Country(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("locale"),
                rs.getString("lang"),
                rs.getString("flag")
        );
    }
}
