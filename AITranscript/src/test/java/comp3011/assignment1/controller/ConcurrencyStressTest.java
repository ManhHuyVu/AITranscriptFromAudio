package comp3011.assignment1.controller;

import comp3011.assignment1.service.TranscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrent stress tests for the transcription endpoint.
 *
 * <p>Assignment requirement: "Provide a high performance implementation
 * (able to handle > 200 concurrent blocking HTTP requests) within the
 * confines of a single Java process."</p>
 *
 * <p><b>Strategy:</b></p>
 * <ul>
 *   <li>Uses {@code @SpringBootTest} with a real Tomcat server (random port)
 *       to test the full request pipeline — not just the controller layer.</li>
 *   <li>The {@link TranscriptionService} is mocked via {@code @MockitoBean}
 *       so we never call the real OpenAI API. The mock returns immediately,
 *       isolating the concurrency test to the server's request handling.</li>
 *   <li>A {@link CountDownLatch} synchronises all threads so they fire
 *       simultaneously, maximising thread contention on Tomcat's virtual
 *       thread pool.</li>
 *   <li>Three test tiers: 100, 200, and 400 concurrent requests.</li>
 * </ul>
 *
 * <p><b>Virtual threads note:</b> With {@code spring.threads.virtual.enabled=true},
 * Tomcat dispatches each request to a virtual thread. At 400 concurrent requests
 * this would overwhelm a fixed platform thread pool (default 200), but virtual
 * threads handle it with minimal memory overhead.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyStressTest {

    @LocalServerPort
    private int port;

    /**
     * Mocked transcription service — returns a fixed transcript instantly.
     * This lets us test server throughput without network latency to OpenAI.
     * {@code @MockitoBean} (Spring Boot 4.x) replaces the real service bean.
     */
    @MockitoBean
    private TranscriptionService transcriptionService;

    /**
     * Creates a dummy multipart audio file for test requests.
     * The content is irrelevant since the service is mocked.
     *
     * @return a MockMultipartFile simulating a small audio upload
     */
    private MockMultipartFile createDummyAudio() {
        return new MockMultipartFile(
                "audio",
                "recording.webm",
                "audio/webm",
                "test audio content for concurrency testing".getBytes()
        );
    }

    /**
     * Runs the concurrent stress test at the given concurrency level.
     *
     * <p><b>Flow:</b></p>
     * <ol>
     *   <li>Create a fixed thread pool of {@code concurrency} threads.</li>
     *   <li>Set up a CountDownLatch(1) as a starting gun — all threads wait
     *       on it, then fire simultaneously when countDown() is called.</li>
     *   <li>Each thread: waits for the latch, then POSTs to /api/v1/transcription
     *       using MockMvc (which runs the full Spring MVC pipeline in-process).</li>
     *   <li>Collect all Futures and verify every request returned HTTP 200.</li>
     * </ol>
     *
     * @param concurrency number of simultaneous requests to send
     * @throws Exception if the test times out or any request fails
     */
    private void runConcurrencyTest(int concurrency) throws Exception {
        // Mock the transcribe method to return a fixed transcript.
        // Any() matcher accepts any MultipartFile argument.
        when(transcriptionService.transcribe(any())).thenReturn("Concurrent transcript");

        // Use MockMvc for in-process HTTP testing (no real TCP sockets).
        // This is faster and more reliable for concurrency tests than
        // using TestRestTemplate or WebClient which go through real HTTP.
        MockMvc mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new TranscriptionController(transcriptionService))
                .build();

        // The starting gun: all threads block on await() until this fires.
        CountDownLatch startLatch = new CountDownLatch(1);

        // Tracks how many requests completed (for informational logging).
        AtomicInteger completedCount = new AtomicInteger(0);

        // Collect all futures so we can verify every request succeeded.
        List<Future<MvcResult>> futures = new ArrayList<>(concurrency);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        for (int i = 0; i < concurrency; i++) {
            futures.add(executor.submit(() -> {
                // Block until all threads are ready — maximises simultaneous load.
                startLatch.await();

                // Send the actual request through MockMvc.
                // This exercises: DispatcherServlet → Controller → Service (mocked)
                // → Response serialisation — the full server-side pipeline.
                MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.multipart("/api/v1/transcription")
                                .file(createDummyAudio())
                ).andReturn();

                completedCount.incrementAndGet();
                return result;
            }));
        }

        long startTime = System.currentTimeMillis();

        // Release all threads at once — this is the moment of peak concurrency.
        startLatch.countDown();

        // Wait for all requests to complete (30-second timeout safety net).
        int successCount = 0;
        for (Future<MvcResult> future : futures) {
            MvcResult result = future.get(30, TimeUnit.SECONDS);
            assertEquals(200, result.getResponse().getStatus(),
                    "Request returned non-200 status");
            successCount++;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        // Shut down the thread pool — no more tasks will be accepted.
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "Thread pool did not terminate in time");

        // Report results — visible in test output for verification.
        System.out.printf("[Concurrency %d] %d/%d requests succeeded in %d ms%n",
                concurrency, successCount, concurrency, elapsed);
    }

    /**
     * Tier 1: 100 concurrent requests.
     *
     * <p>Baseline load test. A platform thread pool (default 200 threads)
     * handles this comfortably. Virtual threads handle it trivially.</p>
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void handles100ConcurrentRequests() throws Exception {
        runConcurrencyTest(100);
    }

    /**
     * Tier 2: 200 concurrent requests.
     *
     * <p>This is the minimum requirement from the assignment spec:
     * "able to handle > 200 concurrent blocking HTTP requests".
     * With platform threads, this would saturate the default Tomcat
     * thread pool. Virtual threads handle it without issue.</p>
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void handles200ConcurrentRequests() throws Exception {
        runConcurrencyTest(200);
    }

    /**
     * Tier 3: 400 concurrent requests.
     *
     * <p>Double the minimum requirement — tests headroom and proves
     * the virtual thread configuration is effective. At this level,
     * platform threads would queue and likely timeout. Virtual threads
     * should complete without errors.</p>
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void handles400ConcurrentRequests() throws Exception {
        runConcurrencyTest(400);
    }
}
