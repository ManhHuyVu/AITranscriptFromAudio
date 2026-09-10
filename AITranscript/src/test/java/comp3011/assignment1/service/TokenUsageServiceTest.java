package comp3011.assignment1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenUsageServiceTest {

    private TokenUsageService tokenUsageService;

    @BeforeEach
    void setUp() {
        tokenUsageService = new TokenUsageService();
    }

    @Test
    void initialStateIsZero() {
        assertEquals(0, tokenUsageService.getInputTokens());
        assertEquals(0, tokenUsageService.getOutputTokens());
    }

    @Test
    void recordUsageSingleCall() {
        tokenUsageService.recordUsage(100, 50);

        assertEquals(100, tokenUsageService.getInputTokens());
        assertEquals(50, tokenUsageService.getOutputTokens());
    }

    @Test
    void recordUsageAccumulates() {
        tokenUsageService.recordUsage(100, 50);
        tokenUsageService.recordUsage(200, 75);
        tokenUsageService.recordUsage(50, 25);

        assertEquals(350, tokenUsageService.getInputTokens());
        assertEquals(150, tokenUsageService.getOutputTokens());
    }

    @Test
    void recordUsageWithZeroTokens() {
        tokenUsageService.recordUsage(0, 0);

        assertEquals(0, tokenUsageService.getInputTokens());
        assertEquals(0, tokenUsageService.getOutputTokens());
    }

    @Test
    void recordUsageWithLargeValues() {
        long largeInput = 1_000_000L;
        long largeOutput = 500_000L;

        tokenUsageService.recordUsage(largeInput, largeOutput);

        assertEquals(largeInput, tokenUsageService.getInputTokens());
        assertEquals(largeOutput, tokenUsageService.getOutputTokens());
    }

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
