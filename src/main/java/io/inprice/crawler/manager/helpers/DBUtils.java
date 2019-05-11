package io.inprice.crawler.manager.helpers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.inprice.crawler.common.config.Config;

import java.sql.Connection;
import java.sql.SQLException;

public class DBUtils {

    private static final HikariDataSource ds;

    static{
        HikariConfig hConf = new HikariConfig();

        hConf.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s", Config.DB_HOST, Config.DB_PORT, Config.DB_DATABASE));
        hConf.setUsername(Config.DB_USERNAME);
        hConf.setPassword(Config.DB_PASSWORD);
        hConf.addDataSourceProperty("cachePrepStmts", "true");
        hConf.addDataSourceProperty("prepStmtCacheSize", "250");
        hConf.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hConf.addDataSourceProperty("useServerPrepStmts", "true");
        hConf.addDataSourceProperty("useLocalSessionState", "true");
        hConf.addDataSourceProperty("rewriteBatchedStatements", "true");
        hConf.addDataSourceProperty("cacheResultSetMetadata", "true");
        hConf.addDataSourceProperty("cacheServerConfiguration", "true");
        hConf.addDataSourceProperty("elideSetAutoCommits", "true");
        hConf.addDataSourceProperty("maintainTimeStats", "false");

        ds = new HikariDataSource(hConf);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static void close() {
        ds.close();
    }

}
