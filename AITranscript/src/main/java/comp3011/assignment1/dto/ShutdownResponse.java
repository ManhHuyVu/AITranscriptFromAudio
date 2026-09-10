package comp3011.assignment1.dto;

/**
 * Response DTO for a successful {@code POST /api/v1/admin/shutdown} request.
 *
 * <p>Serialised to JSON as: {@code {"message": "..."}}</p>
 *
 * @param message a human-readable confirmation that shutdown was requested
 */
public record ShutdownResponse(String message) {
}
