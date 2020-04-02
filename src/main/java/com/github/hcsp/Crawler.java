package com.github.hcsp;

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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Crawler {

    CrawlerDao dao = new MybatisCrawlerDao();

    public void run() throws SQLException, IOException {
        String link;
        //先从数据库加载一个链接(拿出来并从数据库种删掉)，能加载到就循环，并处理之
        while ((link = dao.getNextLinkThenDelete()) != null) {
            //询问数据库当前链接有么有被处理过
            if (dao.isLinkProcessed(link)) {
                continue;
            }
            //关心news.sina.cn
            if (isInterestingLink(link)) {
                System.out.println(link);
                Document doc = httpGetAndParseHtml(link);
                parseUrlsFromPageAndStoneIntoDatabase(doc);
                //如果是新闻页面，那就存入数据库
                stoneIntoDatabaseIfItIsNewsPage(doc, link);
                dao.insertProcessedLink(link);
            }
        }
    }

    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }

    private void parseUrlsFromPageAndStoneIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("javascript") && (!href.isEmpty())) {
                System.out.println("inserLinkToBeProsessed---:+" + href);
                dao.inserLinkToBeProsessed(href);
            }
        }
    }

    private void stoneIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                String content = articleTag.select("p")
                        .stream().map(Element::text).collect(Collectors.joining("\n"));

                dao.insertNewsIntoDatebase(link, title, content);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
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

