package com.readinessaudit.webapp.skills;

import com.readinessaudit.webapp.crawler.CrawlResult;
import com.readinessaudit.webapp.report.Finding;

import java.util.List;

/**
 * Common contract for every audit check (crawlability, schema, freshness,
 * engagement). Each skill receives the same CrawlResult (fetched once,
 * reused everywhere) and returns a list of evidence-backed Findings.
 *
 * The orchestrator just calls run() on each Spring-managed Skill bean and
 * merges the results - adding a new skill means writing one class annotated
 * @Component and implementing this interface; nothing else changes.
 */
public interface Skill {

    /** Machine-readable identifier for this check, used as a JSON key */
    String getName();

    /** One-line human description of what this skill checks */
    String getDescription();

    /** Run this skill's checks against the crawled page and return findings */
    List<Finding> run(CrawlResult crawlResult);
}
