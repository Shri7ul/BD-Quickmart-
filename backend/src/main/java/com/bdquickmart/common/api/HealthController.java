package com.bdquickmart.common.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness endpoint for the platform. Used by the three clients and by
 * deployment tooling to confirm the backend is reachable.
 *
 * <p>This is the LIVENESS endpoint: it always returns HTTP 200 while the
 * application is up, even when PostgreSQL is temporarily unavailable (the
 * database status is reported in the body, never in the HTTP status). Use
 * {@code /actuator/health/readiness} for readiness — that endpoint returns
 * 503 when the database is unreachable.
 *
 * <p>The database check is deliberately bounded: it opens a connection with a
 * short timeout of its own rather than borrowing the pool's, so that a
 * stopped PostgreSQL cannot make this endpoint hang for the full pool
 * timeout. A health check that blocks is worse than one that reports failure.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Service liveness and readiness")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    /** Upper bound on how long the database probe may take. */
    private static final int DB_PROBE_TIMEOUT_MILLIS = 2000;

    private final DataSource dataSource;
    private final String applicationName;

    public HealthController(
            ObjectProvider<DataSource> dataSourceProvider,
            @Value("${spring.application.name}") String applicationName) {
        this.dataSource = dataSourceProvider.getIfAvailable();
        this.applicationName = applicationName;
    }

    @Operation(summary = "Service health check",
            description = "Reports service status and database connectivity.")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "UP");
        payload.put("application", applicationName);
        payload.put("timestamp", java.time.Instant.now().toString());

        Map<String, Object> database = probeDatabase();
        payload.put("database", database.get("status"));
        payload.put("details", database);

        return ResponseEntity.ok(ApiResponse.success(payload, "Service is running",
                "/api/v1/health", null));
    }

    /**
     * Attempts a short-lived connection to the database.
     *
     * <p>Runs on a separate thread so that the HTTP request is never blocked
     * by the JDBC connect timeout, and reports {@code DOWN} with the reason
     * when the database is unreachable.
     */
    private Map<String, Object> probeDatabase() {
        // The probe writes from its own thread while the request thread reads
        // the result, so the map must be thread-safe.
        Map<String, Object> result = new ConcurrentHashMap<>();

        if (dataSource == null) {
            result.put("status", "UNKNOWN");
            result.put("error", "No datasource is configured");
            return result;
        }

        Thread probe = new Thread(() -> {
            long start = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection()) {
                boolean valid = connection.isValid(DB_PROBE_TIMEOUT_MILLIS / 1000);
                result.put("status", valid ? "UP" : "DOWN");
                result.put("latencyMs", System.currentTimeMillis() - start);
                if (!valid) {
                    result.put("error", "Connection validation failed");
                }
            } catch (Exception ex) {
                result.put("status", "DOWN");
                result.put("latencyMs", System.currentTimeMillis() - start);
                result.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
                log.debug("Database health probe failed: {}", ex.getMessage());
            }
        }, "db-health-probe");

        probe.setDaemon(true);
        probe.start();

        try {
            probe.join(DB_PROBE_TIMEOUT_MILLIS + 500L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        if (probe.isAlive()) {
            result.putIfAbsent("status", "DOWN");
            result.putIfAbsent("error", "Database probe timed out after "
                    + DB_PROBE_TIMEOUT_MILLIS + "ms");
        }

        // ConcurrentHashMap has no insertion order; restore it so the payload
        // is stable and readable.
        return new LinkedHashMap<>(result);
    }
}
