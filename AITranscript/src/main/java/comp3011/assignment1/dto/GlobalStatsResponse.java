package comp3011.assignment1.dto;

/**
 * Response DTO for the {@code GET /api/v1/global/stats} endpoint.
 *
 * <p>Serialised to JSON as: {@code {"inputTokens": N, "outputTokens": N}}</p>
 *
 * <p>The field names are camelCase because Java record component names are used
 * directly by Jackson's default property naming strategy. Titan's assessment
 * verifies these exact field names.</p>
 *
 * @param inputTokens  cumulative number of input (prompt) tokens across all requests
 * @param outputTokens cumulative number of output (completion) tokens across all requests
 */
public record GlobalStatsResponse(long inputTokens, long outputTokens) {
}
