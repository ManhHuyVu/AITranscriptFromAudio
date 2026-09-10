package comp3011.assignment1.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link AdminController}.
 *
 * <p><b>{@code @WebMvcTest}</b> loads only the web layer so these tests are fast.
 * No {@code @MockitoBean} is needed here because AdminController has no
 * external service dependencies — it only reads the ApplicationContext
 * (injected by Spring) and uses in-memory state (serverStart, shutdownInProgress).</p>
 *
 * <p><b>Note on shutdown state:</b> The controller is a singleton within the
 * test context. Once the shutdown endpoint is called, {@code shutdownInProgress}
 * stays {@code true} for all subsequent requests in the same test class.
 * This is why we test the 409 (conflict) case by calling shutdown twice.</p>
 */
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies the uptime response contains all three required fields:
     * utcServerStart, utcNow, and serverUptimeSeconds.
     */
    @Test
    void getUptimeReturnsValidResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utcServerStart").exists())
                .andExpect(jsonPath("$.utcNow").exists())
                .andExpect(jsonPath("$.serverUptimeSeconds").isNumber());
    }

    /**
     * Verifies the uptime value is non-negative. The server has been running
     * for some time by the time this test executes, so uptime should be >= 0.
     */
    @Test
    void getUptimeUptimeSecondsIsNonNegative() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverUptimeSeconds").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)));
    }

    /**
     * Tests the idempotency guard on the shutdown endpoint.
     *
     * <p>First POST triggers shutdown (202 Accepted). Second POST should
     * return 409 Conflict because {@code shutdownInProgress} is already true.
     * The {@code AtomicBoolean.compareAndSet} ensures only the first caller wins.</p>
     */
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
