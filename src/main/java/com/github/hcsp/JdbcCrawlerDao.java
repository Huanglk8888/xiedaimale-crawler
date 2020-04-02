package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;

public class JdbcCrawlerDao implements CrawlerDao {
    private static final String username = "root";
    private static final String password = "123456";
    private final Connection connection;



    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection(
                    "jdbc:h2:file:D:/java/produce/yinghe/xiedaimale-crawler/news", username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getNextLink(String sql) throws SQLException {

        ResultSet set = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            set = statement.executeQuery();
            while (set.next()) {
                return set.getString(1);
            }
        } finally {
            if (set != null) {
                set.close();
            }
        }
        return null;
    }

    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink("select LINK from LINKS_TO_BE_PROCESSED LIMIT 1");
        if (link != null) {
            updateDatabase(link, "delete from LINKS_TO_BE_PROCESSED where  LINK = ?");
        }
        return link;
    }

    public void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet set = null;
        try (PreparedStatement statement = connection.prepareStatement("select LINK from LINKS_ALREADY_PROCESSED WHERE  LINK = ?")) {
            statement.setString(1, link);
            set = statement.executeQuery();
            while (set.next()) {
                return true;
            }
        } finally {
            if (set != null) {
                set.close();
            }
        }
        return false;
    }

    public void insertNewsIntoDatebase(String url, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into NEWS (url, TITLE, content, CREATED_AT, MODIFIED_AT) values (?, ?, ?, now(), now())")) {
            statement.setString(1, url);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertProcessedLink(String link) {

    }

    @Override
    public void inserLinkToBeProsessed(String href) {

    }
}
