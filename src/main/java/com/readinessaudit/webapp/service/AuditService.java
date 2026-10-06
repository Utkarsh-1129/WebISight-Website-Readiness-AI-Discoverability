package com.readinessaudit.webapp.service;

import com.readinessaudit.webapp.exception.AuditFailedException;
import com.readinessaudit.webapp.exception.AuditTimeoutException;
import com.readinessaudit.webapp.orchestrator.AuditOrchestrator;
import com.readinessaudit.webapp.report.AuditReport;
import com.readinessaudit.webapp.report.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service layer between the controller and the audit pipeline. Runs each
 * audit as a task on a dedicated bounded thread pool (see AsyncConfig) and
 * enforces a hard wall-clock timeout, so a slow or hanging target site can
 * never tie up a request indefinitely.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditOrchestrator orchestrator;
    private final ReportGenerator reportGenerator;
    private final ExecutorService auditExecutorService;
    private final int timeoutSeconds;

    public AuditService(
            AuditOrchestrator orchestrator,
            ReportGenerator reportGenerator,
            ExecutorService auditExecutorService,
            @Value("${audit.timeout-seconds:30}") int timeoutSeconds) {
        this.orchestrator = orchestrator;
        this.reportGenerator = reportGenerator;
        this.auditExecutorService = auditExecutorService;
        this.timeoutSeconds = timeoutSeconds;
    }

    public AuditReport runAudit(String url) {
        log.info("Starting audit for url={}", url);
        long start = System.currentTimeMillis();

        Future<AuditReport> future = auditExecutorService.submit(() -> {
            AuditOrchestrator.AuditRunResult runResult = orchestrator.audit(url);
            return reportGenerator.generate(runResult);
        });

        try {
            AuditReport report = future.get(timeoutSeconds, TimeUnit.SECONDS);
            log.info("Completed audit for url={} in {}ms, overallScore={}",
                    url, System.currentTimeMillis() - start, report.overallScore);
            return report;

        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AuditTimeoutException(
                    "Audit of " + url + " did not complete within " + timeoutSeconds + " seconds.");

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String causeMessage = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
            log.warn("Audit failed for url={}: {}", url, causeMessage);
            throw new AuditFailedException(
                    "Could not complete the audit for " + url + ": " + causeMessage, cause);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuditFailedException("Audit was interrupted for " + url, e);
        }
    }
}
