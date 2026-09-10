package comp3011.assignment1.dto;

import java.time.Instant;

/**
 * Response DTO for the {@code GET /api/v1/admin/uptime} endpoint.
 *
 * <p>Serialised to JSON as:</p>
 * <pre>{"utcServerStart": "...", "utcNow": "...", "serverUptimeSeconds": N}</pre>
 *
 * <p>Timestamps are ISO-8601 UTC strings produced by {@link Instant} serialisation.</p>
 *
 * @param utcServerStart      UTC timestamp when the server started (bean creation time)
 * @param utcNow              UTC timestamp when the request was handled
 * @param serverUptimeSeconds fractional seconds since server start (e.g. 48.285)
 */
public record UptimeResponse(Instant utcServerStart, Instant utcNow, double serverUptimeSeconds) {
}
