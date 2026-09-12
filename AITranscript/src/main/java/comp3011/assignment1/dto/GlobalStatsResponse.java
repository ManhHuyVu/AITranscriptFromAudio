package comp3011.assignment1.dto;

/**
 * Response DTO for the {@code GET /api/v1/global/stats} endpoint
 *
 * Translate to JSON as: {@code {"inputTokens": N, "outputTokens": N}}
 *
 * @param inputTokens  cumulative number of input (prompt) tokens across all requests
 * @param outputTokens cumulative number of output (completion) tokens across all requests
 */
public record GlobalStatsResponse(long inputTokens, long outputTokens) {
}
