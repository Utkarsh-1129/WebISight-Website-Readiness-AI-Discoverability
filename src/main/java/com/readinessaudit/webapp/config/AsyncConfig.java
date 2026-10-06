package com.readinessaudit.webapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A dedicated, bounded thread pool for running audits, separate from the
 * web server's own request-handling threads. This means a burst of audit
 * requests can never starve Tomcat's HTTP thread pool, and lets the service
 * layer enforce a hard per-audit timeout by running the audit as a task
 * submitted to this executor.
 */
@Configuration
public class AsyncConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService auditExecutorService(
            @Value("${audit.executor.pool-size:8}") int poolSize) {

        ThreadFactory namedThreads = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "audit-worker-" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };

        return Executors.newFixedThreadPool(poolSize, namedThreads);
    }
}
