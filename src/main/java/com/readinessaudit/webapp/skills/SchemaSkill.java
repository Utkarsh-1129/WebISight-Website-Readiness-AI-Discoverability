package com.readinessaudit.webapp.skills;

import com.readinessaudit.webapp.crawler.CrawlResult;
import com.readinessaudit.webapp.report.Finding;
import com.readinessaudit.webapp.report.Finding.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Audits JSON-LD structured data and core metadata tags. AI assistants lean
 * heavily on schema.org JSON-LD to understand what an entity is (Organization,
 * Product, Article, FAQ, etc.) rather than inferring it from prose.
 */
@Component
public class SchemaSkill implements Skill {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() { return "schema-audit"; }

    @Override
    public String getDescription() { return "Audits JSON-LD structured data and core metadata tags."; }

    @Override
    public List<Finding> run(CrawlResult c) {
        List<Finding> findings = new ArrayList<>();

        List<String> jsonLdBlocks = c.getJsonLdBlocks();
        if (jsonLdBlocks == null || jsonLdBlocks.isEmpty()) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("json_ld_present").severity(Severity.HIGH)
                    .passed(false)
                    .issue("No JSON-LD structured data found")
                    .evidence("Zero <script type=\"application/ld+json\"> blocks in the page")
                    .recommendation("Add JSON-LD (schema.org) markup describing the primary entity on this page (Organization, Product, Article, FAQPage, etc.) so AI assistants can extract facts without guessing.")
                    .build());
            findings.addAll(checkMetaTags(c));
            return findings;
        }

        int validBlocks = 0;
        List<String> declaredTypes = new ArrayList<>();
        for (String block : jsonLdBlocks) {
            try {
                JsonNode node = mapper.readTree(block);
                validBlocks++;
                collectTypes(node, declaredTypes);
            } catch (Exception e) {
                findings.add(Finding.builder()
                        .skill(getName()).checkName("json_ld_valid").severity(Severity.HIGH)
                        .passed(false)
                        .issue("A JSON-LD block failed to parse as valid JSON")
                        .evidence("Parse error: " + e.getMessage())
                        .recommendation("Fix the malformed JSON-LD block; invalid JSON is silently ignored by crawlers and AI assistants.")
                        .build());
            }
        }

        if (validBlocks > 0) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("json_ld_present").severity(Severity.INFO)
                    .passed(true)
                    .issue("JSON-LD structured data found")
                    .evidence(validBlocks + " valid block(s); type(s): " + (declaredTypes.isEmpty() ? "unspecified" : String.join(", ", declaredTypes)))
                    .recommendation("No action needed.")
                    .build());
        }

        boolean hasOrgOrWebsite = declaredTypes.stream()
                .anyMatch(t -> t.equalsIgnoreCase("Organization") || t.equalsIgnoreCase("WebSite") || t.equalsIgnoreCase("LocalBusiness"));
        if (!hasOrgOrWebsite) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("organization_schema").severity(Severity.MEDIUM)
                    .passed(false)
                    .issue("No Organization/WebSite schema type detected")
                    .evidence("Declared types: " + (declaredTypes.isEmpty() ? "none" : String.join(", ", declaredTypes)))
                    .recommendation("Add an Organization or WebSite JSON-LD block on key pages (home, about) so AI assistants can reliably identify who runs this site.")
                    .build());
        }

        findings.addAll(checkMetaTags(c));
        return findings;
    }

    private List<Finding> checkMetaTags(CrawlResult c) {
        List<Finding> findings = new ArrayList<>();

        String metaDesc = c.getMetaDescription();
        if (metaDesc == null || metaDesc.isBlank()) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("meta_description").severity(Severity.MEDIUM)
                    .passed(false)
                    .issue("Missing meta description")
                    .evidence("No <meta name=\"description\"> or og:description found")
                    .recommendation("Add a 1-2 sentence meta description summarizing the page; AI assistants and search snippets both rely on it as a fallback summary.")
                    .build());
        } else {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("meta_description").severity(Severity.INFO)
                    .passed(true)
                    .issue("Meta description present")
                    .evidence("\"" + truncate(metaDesc, 100) + "\"")
                    .recommendation("No action needed.")
                    .build());
        }

        boolean hasOgTags = c.getMetaTags().keySet().stream().anyMatch(k -> k.startsWith("og:"));
        if (!hasOgTags) {
            findings.add(Finding.builder()
                    .skill(getName()).checkName("open_graph_tags").severity(Severity.LOW)
                    .passed(false)
                    .issue("No Open Graph tags found")
                    .evidence("No meta properties starting with 'og:'")
                    .recommendation("Add basic Open Graph tags (og:title, og:description, og:image) to improve how the page is represented when shared or cited.")
                    .build());
        }

        return findings;
    }

    private void collectTypes(JsonNode node, List<String> out) {
        if (node.isArray()) {
            for (JsonNode item : node) collectTypes(item, out);
            return;
        }
        if (node.has("@type")) {
            JsonNode typeNode = node.get("@type");
            if (typeNode.isArray()) {
                typeNode.forEach(t -> out.add(t.asText()));
            } else {
                out.add(typeNode.asText());
            }
        }
        if (node.has("@graph")) {
            collectTypes(node.get("@graph"), out);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
