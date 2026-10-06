package com.readinessaudit.webapp.crawler;

import java.util.List;
import java.util.Map;

/**
 * Raw data pulled from a single page fetch.
 * Every Skill reads from this object; nobody re-fetches the page.
 */
public class CrawlResult {

    private final String url;
    private final int statusCode;
    private final long responseTimeMs;
    private final String rawHtml;

    private final String title;
    private final String metaDescription;
    private final List<String> h1Tags;
    private final List<String> h2Tags;
    private final Map<String, String> metaTags;
    private final List<String> jsonLdBlocks;

    private final boolean robotsTxtFound;
    private final boolean robotsAllowsCrawling;
    private final String robotsTxtContent;

    private final List<String> imagesWithoutAlt;
    private final int totalImages;
    private final int totalLinks;
    private final int totalWordCount;

    public CrawlResult(String url, int statusCode, long responseTimeMs, String rawHtml,
                        String title, String metaDescription, List<String> h1Tags, List<String> h2Tags,
                        Map<String, String> metaTags, List<String> jsonLdBlocks,
                        boolean robotsTxtFound, boolean robotsAllowsCrawling, String robotsTxtContent,
                        List<String> imagesWithoutAlt, int totalImages, int totalLinks, int totalWordCount) {
        this.url = url;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        this.rawHtml = rawHtml;
        this.title = title;
        this.metaDescription = metaDescription;
        this.h1Tags = h1Tags;
        this.h2Tags = h2Tags;
        this.metaTags = metaTags;
        this.jsonLdBlocks = jsonLdBlocks;
        this.robotsTxtFound = robotsTxtFound;
        this.robotsAllowsCrawling = robotsAllowsCrawling;
        this.robotsTxtContent = robotsTxtContent;
        this.imagesWithoutAlt = imagesWithoutAlt;
        this.totalImages = totalImages;
        this.totalLinks = totalLinks;
        this.totalWordCount = totalWordCount;
    }

    public String getUrl() { return url; }
    public int getStatusCode() { return statusCode; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public String getRawHtml() { return rawHtml; }
    public String getTitle() { return title; }
    public String getMetaDescription() { return metaDescription; }
    public List<String> getH1Tags() { return h1Tags; }
    public List<String> getH2Tags() { return h2Tags; }
    public Map<String, String> getMetaTags() { return metaTags; }
    public List<String> getJsonLdBlocks() { return jsonLdBlocks; }
    public boolean isRobotsTxtFound() { return robotsTxtFound; }
    public boolean isRobotsAllowsCrawling() { return robotsAllowsCrawling; }
    public String getRobotsTxtContent() { return robotsTxtContent; }
    public List<String> getImagesWithoutAlt() { return imagesWithoutAlt; }
    public int getTotalImages() { return totalImages; }
    public int getTotalLinks() { return totalLinks; }
    public int getTotalWordCount() { return totalWordCount; }
}
