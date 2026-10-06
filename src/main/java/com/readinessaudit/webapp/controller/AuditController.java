package com.readinessaudit.webapp.controller;

import com.readinessaudit.webapp.dto.AuditRequest;
import com.readinessaudit.webapp.report.AuditReport;
import com.readinessaudit.webapp.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for running website AI-discoverability audits.
 *
 * POST /api/audit  { "url": "https://example.com" }  -> AuditReport (JSON)
 */
@RestController
@RequestMapping("/api")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping(value = "/audit", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AuditReport> audit(@Valid @RequestBody AuditRequest request) {
        AuditReport report = auditService.runAudit(request.getUrl());
        return ResponseEntity.ok(report);
    }

    @GetMapping(value = "/ping", produces = "application/json")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("{\"status\":\"ok\"}");
    }
}
