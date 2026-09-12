package comp3011.assignment1.controller;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.service.TokenUsageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the {@code GET /api/v1/global/stats} endpoint

 * Returns the cumulative token usage (input and output) across all successful
 * transcription requests since the server started. The Titan assessment system
 * calls this endpoint before and after transcription to verify token counting
 */
@RestController
public class StatsController {

    private final TokenUsageService tokenUsageService;

    /**
     * Constructor-injected dependency on the shared token usage counter
 
     * @param tokenUsageService the service that tracks input/output token counts
     */
    public StatsController(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    /**
     * Returns the current global token usage statistics
 
     * The response is serialised to JSON automatically by Spring's
     * HttpMessageConverter (Jackson). Field names match the Java record
     * component names: {@code inputTokens} and {@code outputTokens}
   
     * @return a {@link GlobalStatsResponse} containing token counts
     */
    @GetMapping("/api/v1/global/stats")
    public GlobalStatsResponse getGlobalStats() {
        return new GlobalStatsResponse(tokenUsageService.getInputTokens(), tokenUsageService.getOutputTokens());
    }
}
