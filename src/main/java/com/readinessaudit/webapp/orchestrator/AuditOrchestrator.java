package com.readinessaudit.webapp.orchestrator;

import com.readinessaudit.webapp.crawler.CrawlResult;
import com.readinessaudit.webapp.crawler.HttpFetcher;
import com.readinessaudit.webapp.report.Finding;
import com.readinessaudit.webapp.skills.Skill;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates all registered skills against a single crawl of the page.
 *
 * Spring injects every bean that implements Skill automatically (constructor
 * injection of List<Skill>) - adding a new @Component Skill implementation
 * is all that's needed to plug it into the pipeline, no other code changes.
 */
@Component
public class AuditOrchestrator {

    private final HttpFetcher fetcher;
    private final List<Skill> registeredSkills;

    public AuditOrchestrator(HttpFetcher fetcher, List<Skill> registeredSkills) {
        this.fetcher = fetcher;
        this.registeredSkills = registeredSkills;
    }

    /**
     * Runs the full audit pipeline for a single URL: fetch once, run every
     * registered skill against that one CrawlResult, collect the findings.
     */
    public AuditRunResult audit(String url) throws Exception {
        CrawlResult crawlResult = fetcher.fetch(url);

        Map<String, List<Finding>> findingsBySkill = new LinkedHashMap<>();
        for (Skill skill : registeredSkills) {
            List<Finding> findings = skill.run(crawlResult);
            findingsBySkill.put(skill.getName(), findings);
        }

        return new AuditRunResult(crawlResult, findingsBySkill);
    }

    /** Immutable holder for one full audit run: the raw crawl + every skill's findings. */
    public static class AuditRunResult {
        private final CrawlResult crawlResult;
        private final Map<String, List<Finding>> findingsBySkill;

        public AuditRunResult(CrawlResult crawlResult, Map<String, List<Finding>> findingsBySkill) {
            this.crawlResult = crawlResult;
            this.findingsBySkill = findingsBySkill;
        }

        public CrawlResult getCrawlResult() { return crawlResult; }
        public Map<String, List<Finding>> getFindingsBySkill() { return findingsBySkill; }

        public List<Finding> getAllFindings() {
            List<Finding> all = new ArrayList<>();
            findingsBySkill.values().forEach(all::addAll);
            return all;
        }
    }
}
