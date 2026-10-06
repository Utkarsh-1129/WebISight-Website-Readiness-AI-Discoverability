package com.readinessaudit.webapp.skills;

import com.readinessaudit.webapp.crawler.CrawlResult;
import com.readinessaudit.webapp.report.Finding;
import com.readinessaudit.webapp.report.Finding.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Audits stale/consistent facts: date signals and obviously conflicting
 * year references. AI assistants penalize or refuse to cite pages that
 * look outdated or self-contradictory.
 */
@Component
public class FreshnessSkill implements Skill {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern COPYRIGHT_PATTERN = Pattern.compile("(?i)(?:copyright|\u00A9|\\(c\\))\\s*(\\d{4})");

    @Override
    public String getName() { return "freshness-audit"; }

    @Override
    public String getDescription() { return "Audits date signals and fact consistency to detect staleness."; }

    @Override
    public List<Finding> run(CrawlResult c) {
        List<Finding> findings = new ArrayList<>();

        Map<String, String> meta = c.getMetaTags();
        String modified = meta.getOrDefault("article:modified_time", meta.get("article:published_time"));
        if (modified == null || modified.isBlank()) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("last_modified_signal").severity(Severity.MEDIUM)
                    .passed(false)
                    .issue("No last-modified or published-date signal found")
                    .evidence("No article:modified_time / article:published_time meta tag present")
                    .recommendation("Add article:published_time and article:modified_time meta tags (or visible 'Last updated' text) so AI assistants can judge how current the content is.")
                    .build());
        } else {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("last_modified_signal").severity(Severity.INFO)
                    .passed(true)
                    .issue("Date signal present")
                    .evidence(modified)
                    .recommendation("No action needed.")
                    .build());
        }

        Matcher copyrightMatcher = COPYRIGHT_PATTERN.matcher(c.getRawHtml() == null ? "" : c.getRawHtml());
        int currentYear = java.time.Year.now().getValue();
        if (copyrightMatcher.find()) {
            int year = Integer.parseInt(copyrightMatcher.group(1));
            if (year < currentYear - 1) {
                findings.add(Finding.builder()
                        .skill(getName()).checkName("copyright_year_freshness").severity(Severity.LOW)
                        .passed(false)
                        .issue("Copyright year appears stale")
                        .evidence("Found copyright year " + year + "; current year is " + currentYear)
                        .recommendation("Update the footer copyright year, or switch to a dynamic year, so the page doesn't visually signal neglect.")
                        .build());
            }
        }

        List<String> years = new ArrayList<>();
        Matcher yearMatcher = YEAR_PATTERN.matcher(String.join(" ", c.getH1Tags()) + " " + String.join(" ", c.getH2Tags()));
        while (yearMatcher.find()) {
            years.add(yearMatcher.group());
        }
        long distinctYears = years.stream().distinct().count();
        if (distinctYears > 2) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("date_consistency_headings").severity(Severity.LOW)
                    .passed(false)
                    .issue("Multiple different years referenced across headings")
                    .evidence("Years found in H1/H2 text: " + years)
                    .recommendation("Review headings for outdated year references (e.g. old 'Best of 2022' style titles) that make the page look unmaintained.")
                    .build());
        }

        return findings;
    }
}
