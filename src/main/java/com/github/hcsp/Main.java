package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String username = "root";
    private static final String password = "123456";

    private static List<String> loadUrlFromDatabase(Connection connection, String sql) throws SQLException {
        ResultSet set = null;
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            set = statement.executeQuery();
            while (set.next()) {
                results.add(set.getString(1));
            }
        } finally {
            if (set != null) {
                set.close();
            }
        }
        return results;
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:D:/java/produce/yinghe/xiedaimale-crawler/target/news", username, password);

        while (true) {
            //待处理的链接池
            //从数据库加载即将处理的链接的代码
            List<String> linkPool = loadUrlFromDatabase(connection, "select LINK from LINKS_TO_BE_PROCESSED");
            if (linkPool.isEmpty()) {
                break;
            }
            //从待处理池子种拿一个出来
            String link = linkPool.remove(linkPool.size() - 1);
            //处理完后从池子和数据库种删除
            insertLinkIntoDatabase(connection, link, "delete from LINKS_TO_BE_PROCESSED where  LINK = ?");

            //询问数据库当前链接有么有被处理过
            if (isLinkProcessed(connection, link)) {
                continue;
            }
            //关系news.sina.cn
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);

                parseUrlsFromPageAndStoneIntoDatabase(connection, doc);
                //如果是新闻页面，那就存入数据库
                stoneIntoDatabaseIfItIsNewsPage(doc);

                insertLinkIntoDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED (LINK) values (?)");
            }
        }
    }

    private static void parseUrlsFromPageAndStoneIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            insertLinkIntoDatabase(connection, href, "insert into LINKS_TO_BE_PROCESSED (LINK) values (?)");
        }
    }

    //询问数据库当前链接有么有被处理过
    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    //处理完后从池子和数据库种删除
    private static void insertLinkIntoDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    private static void stoneIntoDatabaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        System.out.println(link);
        if (link.startsWith("//")) {
            link = "https:" + link;
            System.out.println(link);
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}

