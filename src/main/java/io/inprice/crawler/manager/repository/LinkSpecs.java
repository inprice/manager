package io.inprice.crawler.manager.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.models.LinkSpec;
import io.inprice.crawler.manager.helpers.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LinkSpecs {

    private static final Logger log = new Logger(LinkSpecs.class);

    public static List<LinkSpec> getAll(Long linkId) {
        final String query = String.format("select * from link_spec where link_id = %d", linkId);

        List<LinkSpec> result = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                LinkSpec linkSpec = new LinkSpec();
                linkSpec.setId(rs.getLong("id"));
                linkSpec.setKey(rs.getString("_key"));
                linkSpec.setValue(rs.getString("_value"));

                result.add(linkSpec);
            }
        } catch (Exception e) {
            log.error("Error in fetching LinkSpecs", e);
        }

        return result;
    }

    public static boolean add(Long linkId, List<Map.Entry<String, String>> specs) {
        final StringBuilder query = new StringBuilder("insert into link_spec (link_id, status) values ");

        for (int i = 0; i < specs.size(); i++) {
            Map.Entry<String, String> spec = specs.get(i);
            query.append(String.format("(%d, '%s', '%s')", linkId, spec.getKey(), spec.getValue()));
            if (i < specs.size() - 1)
                query.append(",");
            else
                query.append(";");
        }

        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query.toString())) {

            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            log.error("Error in adding a new LinkSpec", e);
        }

        return false;
    }

}
