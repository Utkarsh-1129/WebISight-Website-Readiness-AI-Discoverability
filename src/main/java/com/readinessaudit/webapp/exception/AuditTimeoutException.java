package com.readinessaudit.webapp.exception;

/**
 * Thrown when a single audit run exceeds the configured time budget
 * (audit.timeout-seconds), so one slow or unresponsive target site can
 * never hang a server thread indefinitely.
 */
public class AuditTimeoutException extends RuntimeException {
    public AuditTimeoutException(String message) {
        super(message);
    }
}
