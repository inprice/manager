package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.Country;
import io.inprice.crawler.common.models.Site;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class Sites {

    private static final Logger log = new Logger(Sites.class);

    private static final String PLAIN_SEARCH_QUERY = "select * from site ";

    public static List<Site> getAll() {
        return findAll(String.format("%s where s.active = true ", PLAIN_SEARCH_QUERY));
    }

    public static List<Site> search(String term) {
        return findAll(PLAIN_SEARCH_QUERY + " where s.active = true and s.name like '%" + term + "%'");
    }

    public static Site getOne(Long id) {
        List<Site> list = findAll(String.format("%s where s.id = %d", PLAIN_SEARCH_QUERY, id));
        if (list != null && list.size() > 0)
            return list.get(0);
        else
            return null;
    }

    private static List<Site> findAll(String query) {
        List<Site> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (Exception e) {
            log.error("Error in fetching Sites", e);
        }

        return result;
    }

    private static Site map(ResultSet rs) throws SQLException {
        Site site = new Site();
        site.setId(rs.getLong("id"));
        site.setActive(rs.getBoolean("active"));
        site.setName(rs.getString("name"));
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
