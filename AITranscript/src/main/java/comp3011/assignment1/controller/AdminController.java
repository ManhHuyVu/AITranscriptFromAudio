package comp3011.assignment1.controller;

import comp3011.assignment1.dto.ErrorResponse;
import comp3011.assignment1.dto.ShutdownResponse;
import comp3011.assignment1.dto.UptimeResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * REST controller for server administration endpoints:
 * <ul>
 *   <li>{@code GET /api/v1/admin/uptime} — returns server start time, current time, and uptime</li>
 *   <li>{@code POST /api/v1/admin/shutdown} — triggers a graceful shutdown of the Spring context</li>
 * </ul>
 */
@RestController
public class AdminController {

    /** Timestamp captured when this controller bean is created (= server start time). */
    private final Instant serverStart = Instant.now();

    /**
     * Atomic boolean guard that ensures the shutdown endpoint can only be
     * triggered once. Using {@link AtomicBoolean#compareAndSet} makes this
     * thread-safe without explicit synchronisation.
     */
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

    private final ApplicationContext applicationContext;

    /**
     * @param applicationContext the Spring ApplicationContext; needed to call
     *                           {@link SpringApplication#exit(ApplicationContext)}
     */
    public AdminController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Returns server uptime information.
     *
     * @return {@link UptimeResponse} containing UTC timestamps and uptime in seconds
     */
    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getUptime() {
        Instant now = Instant.now();
        // toMillis() / 1000.0 converts to a fractional seconds value (e.g. 48.285)
        double uptimeSeconds = Duration.between(serverStart, now).toMillis() / 1000.0;
        return new UptimeResponse(serverStart, now, uptimeSeconds);
    }

    /**
     * Initiates a graceful shutdown of the Spring Boot application.
     *
     * <p><b>Flow:</b></p>
     * <ol>
     *   <li>First call: sets {@code shutdownInProgress} to true via CAS, then spawns
     *       a daemon thread that sleeps 200 ms (to let the HTTP response flush)
     *       before calling {@link SpringApplication#exit}.</li>
     *   <li>Subsequent calls: returns HTTP 409 Conflict immediately.</li>
     * </ol>
     *
     * <p><b>Why a daemon thread?</b> The 200 ms sleep and {@code SpringApplication.exit()}
     * must not block the request thread (which is still sending the 202 response).
     * A daemon thread ensures the JVM can still exit even if this thread is
     * still running when non-daemon threads finish.</p>
     *
     * @return 202 Accepted on first call, 409 Conflict on repeated calls
     */
    @PostMapping("/api/v1/admin/shutdown")
    public ResponseEntity<?> shutdown() {
        // compareAndSet is atomic: only the first caller sees false→true.
        if (!shutdownInProgress.compareAndSet(false, true)) {
            ErrorResponse error = new ErrorResponse(
                    Instant.now(),
                    409,
                    "Conflict",
                    "Graceful shutdown is already in progress.",
                    "/api/v1/admin/shutdown"
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        Thread shutdownThread = new Thread(() -> {
            try {
                // Small delay to let the HTTP 202 response reach the client
                // before we start tearing down the Tomcat connector.
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            SpringApplication.exit(applicationContext, () -> 0);
        });
        shutdownThread.setDaemon(true);
        shutdownThread.start();

        return ResponseEntity.accepted().body(new ShutdownResponse("Graceful shutdown requested."));
    }
}
