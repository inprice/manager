package io.inprice.scrapper.manager.helpers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.inprice.scrapper.common.config.Config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

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

    public static Connection getTransactionalConnection() throws SQLException {
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        return con;
    }

    public static void rollback(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ex) {
            //
        }
    }

    public static void close(Connection con, Statement pst) {
        try {
            pst.close();
            con.close();
        } catch (SQLException ex) {
            //
        }
    }

    public static void shutdown() {
        ds.close();
    }

}
