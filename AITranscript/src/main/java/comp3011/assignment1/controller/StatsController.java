package comp3011.assignment1.controller;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.service.TokenUsageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final TokenUsageService tokenUsageService;

    public StatsController(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @GetMapping("/api/v1/global/stats")
    public GlobalStatsResponse getGlobalStats() {
        return new GlobalStatsResponse(tokenUsageService.getInputTokens(), tokenUsageService.getOutputTokens());
    }
}