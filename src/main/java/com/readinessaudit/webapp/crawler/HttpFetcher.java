package com.readinessaudit.webapp.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches a page's HTML and robots.txt, then parses the core signals every
 * skill needs (title, headings, metadata, JSON-LD, images, word count).
 *
 * No JavaScript is executed here on purpose: most AI crawlers (GPTBot,
 * ClaudeBot, PerplexityBot) don't execute JS either, so this is a faithful
 * simulation of what they actually see.
 */
@Component
public class HttpFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; AIReadinessAuditBot/1.0; +https://example.com/bot)";

    private final HttpClient httpClient;

    public HttpFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public CrawlResult fetch(String url) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long elapsed = System.currentTimeMillis() - start;

        String html = response.body();
        Document doc = Jsoup.parse(html, url);

        String title = doc.title();
        String metaDescription = extractMetaContent(doc, "description");

        List<String> h1Tags = extractTagText(doc, "h1");
        List<String> h2Tags = extractTagText(doc, "h2");

        Map<String, String> metaTags = extractAllMeta(doc);
        List<String> jsonLdBlocks = extractJsonLd(doc);

        List<String> imagesWithoutAlt = findImagesMissingAlt(doc);
        int totalImages = doc.select("img").size();
        int totalLinks = doc.select("a[href]").size();
        int totalWordCount = countWords(doc);

        RobotsResult robots = fetchRobotsTxt(url);

        return new CrawlResult(
                url,
                response.statusCode(),
                elapsed,
                html,
                title,
                metaDescription,
                h1Tags,
                h2Tags,
                metaTags,
                jsonLdBlocks,
                robots.found(),
                robots.allowsCrawling(),
                robots.content(),
                imagesWithoutAlt,
                totalImages,
                totalLinks,
                totalWordCount
        );
    }

    private String extractMetaContent(Document doc, String name) {
        Element meta = doc.selectFirst("meta[name=" + name + "]");
        if (meta == null) {
            meta = doc.selectFirst("meta[property=og:" + name + "]");
        }
        return meta != null ? meta.attr("content") : "";
    }

    private List<String> extractTagText(Document doc, String tag) {
        List<String> results = new ArrayList<>();
        Elements elements = doc.select(tag);
        for (Element el : elements) {
            String text = el.text().trim();
            if (!text.isEmpty()) {
                results.add(text);
            }
        }
        return results;
    }

    private Map<String, String> extractAllMeta(Document doc) {
        Map<String, String> metaMap = new LinkedHashMap<>();
        for (Element meta : doc.select("meta")) {
            String key = meta.hasAttr("name") ? meta.attr("name")
                    : meta.hasAttr("property") ? meta.attr("property")
                    : null;
            if (key != null && !key.isBlank()) {
                metaMap.put(key, meta.attr("content"));
            }
        }
        return metaMap;
    }

    private List<String> extractJsonLd(Document doc) {
        List<String> blocks = new ArrayList<>();
        for (Element script : doc.select("script[type=application/ld+json]")) {
            String data = script.data().trim();
            if (!data.isEmpty()) {
                blocks.add(data);
            }
        }
        return blocks;
    }

    private List<String> findImagesMissingAlt(Document doc) {
        List<String> missing = new ArrayList<>();
        for (Element img : doc.select("img")) {
            String alt = img.attr("alt");
            if (alt.isBlank()) {
                missing.add(img.hasAttr("src") ? img.attr("src") : "(no src)");
            }
        }
        return missing;
    }

    private int countWords(Document doc) {
        String bodyText = doc.body() != null ? doc.body().text() : "";
        if (bodyText.isBlank()) return 0;
        return bodyText.trim().split("\\s+").length;
    }

    private RobotsResult fetchRobotsTxt(String pageUrl) {
        try {
            URI base = URI.create(pageUrl);
            String robotsUrl = base.getScheme() + "://" + base.getHost()
                    + (base.getPort() > 0 ? ":" + base.getPort() : "") + "/robots.txt";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(robotsUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                String content = resp.body();
                boolean allows = !containsDisallowAll(content);
                return new RobotsResult(true, allows, content);
            }
            return new RobotsResult(false, true, "");
        } catch (Exception e) {
            // If robots.txt is unreachable, assume crawling is allowed (common convention)
            return new RobotsResult(false, true, "");
        }
    }

    private boolean containsDisallowAll(String robotsContent) {
        boolean sawWildcardUserAgent = false;
        for (String line : robotsContent.split("\\r?\\n")) {
            String trimmed = line.trim().toLowerCase();
            if (trimmed.startsWith("user-agent:")) {
                sawWildcardUserAgent = trimmed.contains("*");
            } else if (sawWildcardUserAgent && trimmed.startsWith("disallow:")) {
                String path = trimmed.substring("disallow:".length()).trim();
                if (path.equals("/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private record RobotsResult(boolean found, boolean allowsCrawling, String content) {}
}
