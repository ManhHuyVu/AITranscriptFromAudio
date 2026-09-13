package comp3011.assignment1.controller;

import comp3011.assignment1.service.TokenUsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link StatsController}.
 *
 * <p><b>{@code @WebMvcTest}</b> loads only the web layer — the controller, Jackson
 * serialisation, and MockMvc — without starting the full Spring context or
 * hitting a real database. This makes these tests fast and focused.</p>
 *
 * <p><b>{@code @MockitoBean}</b> (Spring Boot 4.x replacement for the old
 * {@code @MockBean}) injects a Mockito mock for {@link TokenUsageService},
 * letting us control the return values of getInputTokens/getOutputTokens
 * without any real state.</p>
 *
 * <p>The tests verify:</p>
 * <ul>
 *   <li>HTTP 200 status for GET /api/v1/global/stats</li>
 *   <li>Correct JSON field names and values</li>
 *   <li>Initial state returns zeros</li>
 * </ul>
 */
@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Mocked token usage service — controls what the controller reads */
    @MockitoBean
    private TokenUsageService tokenUsageService;

    /**
     * Verifies the response contains the mocked token values
     * The JSON path {@code $.inputTokens} maps to the record field name
     */
    @Test
    void getGlobalStatsReturnsTokenUsage() throws Exception {
        when(tokenUsageService.getInputTokens()).thenReturn(150L);
        when(tokenUsageService.getOutputTokens()).thenReturn(75L);

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").value(150))
                .andExpect(jsonPath("$.outputTokens").value(75));
    }

    /**
     * Simulates the state before any transcription has occurred
     * Titan checks this endpoint before recording audio
     */
    @Test
    void getGlobalStatsReturnsZerosInitially() throws Exception {
        when(tokenUsageService.getInputTokens()).thenReturn(0L);
        when(tokenUsageService.getOutputTokens()).thenReturn(0L);

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").value(0))
                .andExpect(jsonPath("$.outputTokens").value(0));
    }

    /**
     * Structural test: ensures both required fields exist in the JSON even when values are zero
     * Titan uses this to validate the response shape
     */
    @Test
    void getGlobalStatsReturnsCorrectJsonStructure() throws Exception {
        when(tokenUsageService.getInputTokens()).thenReturn(0L);
        when(tokenUsageService.getOutputTokens()).thenReturn(0L);

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").exists())
                .andExpect(jsonPath("$.outputTokens").exists());
    }
}
