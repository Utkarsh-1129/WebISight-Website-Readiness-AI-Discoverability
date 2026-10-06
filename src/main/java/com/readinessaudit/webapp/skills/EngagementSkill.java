package com.readinessaudit.webapp.skills;

import com.readinessaudit.webapp.crawler.CrawlResult;
import com.readinessaudit.webapp.report.Finding;
import com.readinessaudit.webapp.report.Finding.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Audits UX and content clarity: heading structure, alt text coverage,
 * content depth, and link density. A page can be perfectly machine-readable
 * and still fail visitors if it is structurally unclear or inaccessible.
 */
@Component
public class EngagementSkill implements Skill {

    @Override
    public String getName() { return "engagement-audit"; }

    @Override
    public String getDescription() { return "Audits UX and content clarity: heading structure, alt text, content depth."; }

    @Override
    public List<Finding> run(CrawlResult c) {
        List<Finding> findings = new ArrayList<>();

        int h1Count = c.getH1Tags() != null ? c.getH1Tags().size() : 0;
        if (h1Count == 0) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("h1_structure").severity(Severity.HIGH)
                    .passed(false)
                    .issue("No H1 heading found")
                    .evidence("0 <h1> tags detected")
                    .recommendation("Add a single, clear H1 that states the page's main topic - it anchors both SEO and how AI assistants summarize the page.")
                    .build());
        } else if (h1Count > 1) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("h1_structure").severity(Severity.LOW)
                    .passed(false)
                    .issue("Multiple H1 headings found")
                    .evidence(h1Count + " <h1> tags: " + c.getH1Tags())
                    .recommendation("Keep a single H1 per page and demote the rest to H2/H3 to keep the content hierarchy unambiguous.")
                    .build());
        } else {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("h1_structure").severity(Severity.INFO)
                    .passed(true)
                    .issue("Exactly one H1 heading found")
                    .evidence("\"" + c.getH1Tags().get(0) + "\"")
                    .recommendation("No action needed.")
                    .build());
        }

        if (c.getTotalWordCount() > 500 && (c.getH2Tags() == null || c.getH2Tags().isEmpty())) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("content_structure").severity(Severity.MEDIUM)
                    .passed(false)
                    .issue("Long page with no subheadings")
                    .evidence(c.getTotalWordCount() + " words but 0 <h2> tags")
                    .recommendation("Break long content into sections with H2 subheadings; this helps both readers scan and AI assistants extract specific answerable chunks.")
                    .build());
        }

        int totalImages = c.getTotalImages();
        int missingAlt = c.getImagesWithoutAlt() != null ? c.getImagesWithoutAlt().size() : 0;
        if (totalImages > 0) {
            double missingRatio = (double) missingAlt / totalImages;
            if (missingRatio > 0.2) {
                findings.add(Finding.builder()
                        .skill(getName()).checkName("image_alt_text").severity(Severity.MEDIUM)
                        .passed(false)
                        .issue("Significant portion of images missing alt text")
                        .evidence(missingAlt + " of " + totalImages + " images have no alt attribute")
                        .recommendation("Add descriptive alt text to images; it's an accessibility requirement and gives AI assistants (which mostly can't see images) textual context.")
                        .build());
            } else {
                findings.add(Finding.builder()
                        .skill(getName()).checkName("image_alt_text").severity(Severity.INFO)
                        .passed(true)
                        .issue("Most images have alt text")
                        .evidence((totalImages - missingAlt) + " of " + totalImages + " images have alt attributes")
                        .recommendation("No action needed.")
                        .build());
            }
        }

        if (c.getTotalWordCount() < 100) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("content_depth").severity(Severity.MEDIUM)
                    .passed(false)
                    .issue("Very thin content on this page")
                    .evidence(c.getTotalWordCount() + " visible words")
                    .recommendation("Expand the page with substantive content; very thin pages are commonly deprioritized by both search and AI-assistant retrieval.")
                    .build());
        }

        if (c.getTotalWordCount() > 0) {
            double linksPerHundredWords = (c.getTotalLinks() * 100.0) / c.getTotalWordCount();
            if (linksPerHundredWords > 30) {
                findings.add(Finding.builder()
                        .skill(getName()).checkName("link_density").severity(Severity.LOW)
                        .passed(false)
                        .issue("Very high link density relative to content")
                        .evidence(c.getTotalLinks() + " links across " + c.getTotalWordCount() + " words")
                        .recommendation("Reduce link density or add more original content between links; pages that read as mostly navigation/links are seen as lower quality.")
                        .build());
            }
        }

        return findings;
    }
}
