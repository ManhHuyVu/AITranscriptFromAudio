package comp3011.assignment1.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getUptimeReturnsValidResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utcServerStart").exists())
                .andExpect(jsonPath("$.utcNow").exists())
                .andExpect(jsonPath("$.serverUptimeSeconds").isNumber());
    }

    @Test
    void getUptimeUptimeSecondsIsNonNegative() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverUptimeSeconds").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)));
    }

    @Test
    void shutdownEndpointReturnsConflictWhenAlreadyInProgress() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shutdown"));
        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Graceful shutdown is already in progress."));
    }
}
