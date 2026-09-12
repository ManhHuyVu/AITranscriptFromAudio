package comp3011.assignment1.dto;

/**
 * Response DTO for a successful {@code POST /api/v1/admin/shutdown} request
 *
 * Translate to JSON as: {@code {"message": "..."}}
 *
 * @param message a human-readable confirmation that shutdown was requested
 */
public record ShutdownResponse(String message) {
}
