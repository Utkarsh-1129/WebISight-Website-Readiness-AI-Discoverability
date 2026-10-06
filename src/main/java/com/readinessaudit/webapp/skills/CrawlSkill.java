package com.readinessaudit.webapp.skills;

import com.readinessaudit.webapp.crawler.CrawlResult;
import com.readinessaudit.webapp.report.Finding;
import com.readinessaudit.webapp.report.Finding.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Audits crawlability & rendering: robots.txt, HTTP status, title tag,
 * response time, and whether the page appears to depend on client-side
 * JavaScript to render its main content.
 */
@Component
public class CrawlSkill implements Skill {

    @Override
    public String getName() { return "crawl-render-audit"; }

    @Override
    public String getDescription() { return "Audits crawlability & rendering: robots.txt, status codes, JS-dependency."; }

    @Override
    public List<Finding> run(CrawlResult c) {
        List<Finding> findings = new ArrayList<>();

        if (c.getStatusCode() >= 200 && c.getStatusCode() < 300) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("http_status").severity(Severity.INFO)
                    .passed(true)
                    .issue("Page returned a healthy HTTP status")
                    .evidence("HTTP " + c.getStatusCode())
                    .recommendation("No action needed.")
                    .build());
        } else {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("http_status").severity(Severity.CRITICAL)
                    .passed(false)
                    .issue("Page did not return a successful HTTP status")
                    .evidence("HTTP " + c.getStatusCode())
                    .recommendation("Fix the server/redirect chain so this URL returns 200 OK; AI crawlers typically drop non-200 pages.")
                    .build());
        }

        if (!c.isRobotsTxtFound()) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("robots_txt_present").severity(Severity.MEDIUM)
                    .passed(false)
                    .issue("No robots.txt file found")
                    .evidence("/robots.txt did not return HTTP 200")
                    .recommendation("Add a robots.txt at the domain root explicitly allowing reputable AI crawlers (GPTBot, ClaudeBot, PerplexityBot, Google-Extended) unless you intend to block them.")
                    .build());
        } else if (!c.isRobotsAllowsCrawling()) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("robots_txt_allows_crawling").severity(Severity.CRITICAL)
                    .passed(false)
                    .issue("robots.txt disallows all crawlers on this path")
                    .evidence(truncate(c.getRobotsTxtContent(), 300))
                    .recommendation("Remove the blanket 'Disallow: /' for User-agent: * unless blocking all bots is intentional, or add explicit Allow rules for AI crawlers you want to be discoverable by.")
                    .build());
        } else {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("robots_txt_allows_crawling").severity(Severity.INFO)
                    .passed(true)
                    .issue("robots.txt permits crawling")
                    .evidence("No blanket disallow rule found for User-agent: *")
                    .recommendation("No action needed.")
                    .build());
        }

        String title = c.getTitle();
        if (title == null || title.isBlank()) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("title_tag").severity(Severity.HIGH)
                    .passed(false)
                    .issue("Missing <title> tag")
                    .evidence("Title was empty")
                    .recommendation("Add a concise, descriptive <title> - it's one of the strongest signals AI assistants use to identify what a page is about.")
                    .build());
        } else if (title.length() > 70) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("title_tag").severity(Severity.LOW)
                    .passed(false)
                    .issue("Title tag is unusually long")
                    .evidence("Title (" + title.length() + " chars): \"" + truncate(title, 80) + "\"")
                    .recommendation("Trim the title to under ~60-70 characters so it isn't truncated in search/AI citations.")
                    .build());
        } else {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("title_tag").severity(Severity.INFO)
                    .passed(true)
                    .issue("Title tag present and well-sized")
                    .evidence("\"" + title + "\"")
                    .recommendation("No action needed.")
                    .build());
        }

        int htmlLength = c.getRawHtml() != null ? c.getRawHtml().length() : 0;
        if (htmlLength > 2000 && c.getTotalWordCount() < 50) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("js_rendering_dependency").severity(Severity.CRITICAL)
                    .passed(false)
                    .issue("Page likely renders its content client-side via JavaScript")
                    .evidence("Raw HTML is " + htmlLength + " chars but only " + c.getTotalWordCount() + " visible words were found without executing JS")
                    .recommendation("Most AI crawlers (GPTBot, ClaudeBot) do not execute JavaScript. Use server-side rendering (SSR) or static generation for critical content, or add a prerendered fallback for bots.")
                    .build());
        } else {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("js_rendering_dependency").severity(Severity.INFO)
                    .passed(true)
                    .issue("Page content is present in server-rendered HTML")
                    .evidence(c.getTotalWordCount() + " visible words found without executing JS")
                    .recommendation("No action needed.")
                    .build());
        }

        if (c.getResponseTimeMs() > 3000) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("response_time").severity(Severity.MEDIUM)
                    .passed(false)
                    .issue("Slow server response time")
                    .evidence(c.getResponseTimeMs() + "ms to first byte")
                    .recommendation("Crawl budgets are limited; a slow response increases the chance a bot times out before indexing the page. Target under 1000ms.")
                    .build());
        }

        return findings;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
