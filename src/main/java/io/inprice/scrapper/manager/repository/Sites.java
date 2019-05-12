package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Sites {

    private static final Logger log = new Logger(Sites.class);

    public static List<Site> getAll() {
        final String query = "select * from site where active = true ";

        List<Site> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Failed to fetch Sites", e);
        }

        return result;
    }

    private static Site map(ResultSet rs) throws SQLException {
        Site site = new Site();
        site.setId(rs.getLong("id"));
        site.setActive(rs.getBoolean("active"));
        site.setName(rs.getString("name"));
        site.setDomain(rs.getString("domain"));
        site.setUrl(rs.getString("url"));
        site.setLogo(rs.getString("logo"));
        site.setLogoMini(rs.getString("logo_mini"));
        site.setCurrencyCode(rs.getString("currency_code"));
        site.setCurrencySymbol(rs.getString("currency_symbol"));
        site.setThousandSeparator(rs.getString("thousand_separator"));
        site.setDecimalSeparator(rs.getString("decimal_separator"));
        site.setClassName(rs.getString("class_name"));
        site.setInsertAt(rs.getDate("insert_at"));

        site.setCountryId(rs.getLong("country_id"));

        return site;
    }
}
