package comp3011.assignment1.dto;

import java.time.Instant;

/**
 * Standardised error response DTO returned when an API endpoint fails
 *
 * Translate to JSON as:
 * {"timestamp": "...", "status": 502, "error": "Bad Gateway", 
 * "message": "...", "path": "/api/v1/transcription"}
 *
 * This mirrors the default Spring Boot error format so that the frontend
 * can parse error details consistently
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code (e.g. 502, 409)
 * @param error     HTTP reason phrase
 * @param message   human-readable error description (may include the upstream error)
 * @param path      the request URI that caused the error
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}
