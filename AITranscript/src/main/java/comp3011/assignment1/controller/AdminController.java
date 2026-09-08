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

@RestController
public class AdminController {

    private final Instant serverStart = Instant.now();
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final ApplicationContext applicationContext;

    public AdminController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getUptime() {
        Instant now = Instant.now();
        double uptimeSeconds = Duration.between(serverStart, now).toMillis() / 1000.0;
        return new UptimeResponse(serverStart, now, uptimeSeconds);
    }

    @PostMapping("/api/v1/admin/shutdown")
    public ResponseEntity<?> shutdown() {
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
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            SpringApplication.exit(applicationContext, () -> 0);
        });
        shutdownThread.start();

        return ResponseEntity.accepted().body(new ShutdownResponse("Graceful shutdown requested."));
    }
}