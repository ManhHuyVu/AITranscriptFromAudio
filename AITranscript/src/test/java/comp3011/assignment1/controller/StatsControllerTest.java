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

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenUsageService tokenUsageService;

    @Test
    void getGlobalStatsReturnsTokenUsage() throws Exception {
        when(tokenUsageService.getInputTokens()).thenReturn(150L);
        when(tokenUsageService.getOutputTokens()).thenReturn(75L);

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").value(150))
                .andExpect(jsonPath("$.outputTokens").value(75));
    }

    @Test
    void getGlobalStatsReturnsZerosInitially() throws Exception {
        when(tokenUsageService.getInputTokens()).thenReturn(0L);
        when(tokenUsageService.getOutputTokens()).thenReturn(0L);

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").value(0))
                .andExpect(jsonPath("$.outputTokens").value(0));
    }

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
