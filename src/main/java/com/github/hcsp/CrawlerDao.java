package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {

    String getNextLinkThenDelete() throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDatebase(String url, String title, String content) throws SQLException;

    void insertProcessedLink(String link) throws SQLException;

    void inserLinkToBeProsessed(String href) throws SQLException;
}
