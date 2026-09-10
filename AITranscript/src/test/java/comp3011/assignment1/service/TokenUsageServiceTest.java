package comp3011.assignment1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link TokenUsageService}.
 *
 * <p>These are pure unit tests — no Spring context is loaded, so they run
 * in milliseconds. The service is instantiated directly in {@link #setUp()}.</p>
 *
 * <p><b>Test categories:</b></p>
 * <ul>
 *   <li>Initial state verification</li>
 *   <li>Single and accumulated recording</li>
 *   <li>Edge cases (zero values, large values)</li>
 *   <li>Concurrent access thread-safety</li>
 * </ul>
 */
class TokenUsageServiceTest {

    private TokenUsageService tokenUsageService;

    /**
     * Creates a fresh service instance before each test to avoid state leakage.
     */
    @BeforeEach
    void setUp() {
        tokenUsageService = new TokenUsageService();
    }

    /**
     * Verifies that both counters start at zero.
     * This is important because Titan checks the stats endpoint
     * before any transcription occurs.
     */
    @Test
    void initialStateIsZero() {
        assertEquals(0, tokenUsageService.getInputTokens());
        assertEquals(0, tokenUsageService.getOutputTokens());
    }

    /**
     * Verifies that a single call to recordUsage correctly sets both counters.
     */
    @Test
    void recordUsageSingleCall() {
        tokenUsageService.recordUsage(100, 50);

        assertEquals(100, tokenUsageService.getInputTokens());
        assertEquals(50, tokenUsageService.getOutputTokens());
    }

    /**
     * Verifies that multiple calls accumulate correctly.
     * Simulates three consecutive transcription requests.
     */
    @Test
    void recordUsageAccumulates() {
        tokenUsageService.recordUsage(100, 50);
        tokenUsageService.recordUsage(200, 75);
        tokenUsageService.recordUsage(50, 25);

        assertEquals(350, tokenUsageService.getInputTokens());
        assertEquals(150, tokenUsageService.getOutputTokens());
    }

    /**
     * Edge case: recording zero tokens should not change the counters.
     * This could happen if the OpenAI response somehow reported zero usage.
     */
    @Test
    void recordUsageWithZeroTokens() {
        tokenUsageService.recordUsage(0, 0);

        assertEquals(0, tokenUsageService.getInputTokens());
        assertEquals(0, tokenUsageService.getOutputTokens());
    }

    /**
     * Edge case: very large token counts that might occur with long audio files.
     * Ensures no overflow or precision loss.
     */
    @Test
    void recordUsageWithLargeValues() {
        long largeInput = 1_000_000L;
        long largeOutput = 500_000L;

        tokenUsageService.recordUsage(largeInput, largeOutput);

        assertEquals(largeInput, tokenUsageService.getInputTokens());
        assertEquals(largeOutput, tokenUsageService.getOutputTokens());
    }

    /**
     * Stress-tests thread safety with 10 threads each performing 100 increments.
     *
     * <p><b>Why this matters:</b> The app uses virtual threads
     * ({@code spring.threads.virtual.enabled=true}), so many transcription
     * requests can call recordUsage concurrently. If AtomicLong were replaced
     * with a plain long, this test would fail with lost updates.</p>
     *
     * <p><b>How it works:</b></p>
     * <ol>
     *   <li>A fixed thread pool of 10 threads is created.</li>
     *   <li>A {@link CountDownLatch} synchronises the start so all threads
     *       begin at roughly the same time (maximising contention).</li>
     *   <li>Each thread increments both counters 100 times (1 token per call).</li>
     *   <li>We await the latch with a 10-second timeout to prevent the test
     *       from hanging if something goes wrong.</li>
     *   <li>Final assertion: total = 10 threads x 100 ops = 1000.</li>
     * </ol>
     */
    @Test
    void concurrentRecordUsage() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        tokenUsageService.recordUsage(1, 1);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long expected = (long) threadCount * operationsPerThread;
        assertEquals(expected, tokenUsageService.getInputTokens());
        assertEquals(expected, tokenUsageService.getOutputTokens());
    }
}
