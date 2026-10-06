package com.readinessaudit.webapp.exception;

/**
 * Thrown when the audit pipeline itself fails - typically an unreachable
 * host, connection refused, or a network-level IOException from HttpFetcher.
 */
public class AuditFailedException extends RuntimeException {
    public AuditFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
