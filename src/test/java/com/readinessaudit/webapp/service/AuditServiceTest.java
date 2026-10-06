package com.readinessaudit.webapp.service;

import com.readinessaudit.webapp.exception.AuditFailedException;
import com.readinessaudit.webapp.exception.AuditTimeoutException;
import com.readinessaudit.webapp.orchestrator.AuditOrchestrator;
import com.readinessaudit.webapp.report.AuditReport;
import com.readinessaudit.webapp.report.ReportGenerator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    @Test
    void wrapsSlowAuditsInAuditTimeoutException() throws Exception {
        AuditOrchestrator orchestrator = mock(AuditOrchestrator.class);
        ReportGenerator reportGenerator = new ReportGenerator();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Simulate a pipeline that never returns within the timeout window
        when(orchestrator.audit(anyString())).thenAnswer(invocation -> {
            TimeUnit.SECONDS.sleep(5);
            return null;
        });

        AuditService service = new AuditService(orchestrator, reportGenerator, executor, 1);

        assertThrows(AuditTimeoutException.class, () -> service.runAudit("https://slow.example"));
        executor.shutdownNow();
    }

    @Test
    void wrapsPipelineFailuresInAuditFailedException() throws Exception {
        AuditOrchestrator orchestrator = mock(AuditOrchestrator.class);
        ReportGenerator reportGenerator = new ReportGenerator();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        when(orchestrator.audit(anyString())).thenThrow(new java.io.IOException("connection refused"));

        AuditService service = new AuditService(orchestrator, reportGenerator, executor, 5);

        assertThrows(AuditFailedException.class, () -> service.runAudit("https://unreachable.example"));
        executor.shutdown();
    }

    @Test
    void returnsGeneratedReportOnSuccess() throws Exception {
        AuditOrchestrator orchestrator = mock(AuditOrchestrator.class);
        ReportGenerator reportGenerator = mock(ReportGenerator.class);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AuditOrchestrator.AuditRunResult runResult = mock(AuditOrchestrator.AuditRunResult.class);
        AuditReport expectedReport = new AuditReport();
        expectedReport.overallScore = 90;

        when(orchestrator.audit("https://good.example")).thenReturn(runResult);
        when(reportGenerator.generate(runResult)).thenReturn(expectedReport);

        AuditService service = new AuditService(orchestrator, reportGenerator, executor, 5);
        AuditReport actual = service.runAudit("https://good.example");

        assertEquals(90, actual.overallScore);
        executor.shutdown();
    }
}
