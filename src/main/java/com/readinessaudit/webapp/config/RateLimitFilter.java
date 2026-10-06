package com.readinessaudit.webapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight per-IP rate limiter for the audit endpoint. Each outbound
 * audit makes real network calls to a third-party site, so this endpoint
 * is more expensive per-request than a typical API call and worth guarding
 * against accidental or deliberate hammering.
 *
 * This is an in-memory, single-instance limiter (a fixed window per minute,
 * reset by a scheduled sweep) - adequate for a single deployment. A
 * multi-instance deployment behind a load balancer would need a shared
 * store (e.g. Redis) instead; see the audit.rate-limit.* properties.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private volatile long windowStartMillis = System.currentTimeMillis();

    private final int maxRequestsPerWindow;
    private final long windowMillis;

    public RateLimitFilter(
            @Value("${audit.rate-limit.max-requests-per-minute:20}") int maxRequestsPerWindow) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMillis = 60_000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only the audit endpoint makes outbound network calls; static
        // assets and health checks are cheap and left unthrottled.
        return !request.getRequestURI().startsWith("/api/audit");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        resetWindowIfExpired();

        String clientIp = resolveClientIp(request);
        int currentCount = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0))
                .incrementAndGet();

        if (currentCount > maxRequestsPerWindow) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded, try again shortly.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private synchronized void resetWindowIfExpired() {
        long now = System.currentTimeMillis();
        if (now - windowStartMillis > windowMillis) {
            requestCounts.clear();
            windowStartMillis = now;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
