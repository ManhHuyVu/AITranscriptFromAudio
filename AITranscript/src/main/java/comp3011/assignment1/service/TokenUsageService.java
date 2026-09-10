package comp3011.assignment1.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe in-memory counter for cumulative OpenAI token usage.
 *
 * <p><b>Why {@link AtomicLong}?</b></p>
 * <p>With virtual threads enabled ({@code spring.threads.virtual.enabled=true}),
 * every concurrent transcription request runs on its own virtual thread and calls
 * {@link #recordUsage} concurrently. A plain {@code long} field would lose updates
 * due to data races. {@code AtomicLong.addAndGet()} provides a lock-free,
 * CAS-based atomic increment that is safe under high concurrency.</p>
 *
 * <p>The counters are reset only when the JVM restarts (i.e. the JAR is relaunched).</p>
 */
@Service
public class TokenUsageService {

	private final AtomicLong inputTokens = new AtomicLong(0);
	private final AtomicLong outputTokens = new AtomicLong(0);

	/**
	 * Accumulates token counts after a successful OpenAI transcription.
	 *
	 * @param input  number of input tokens consumed by the request
	 * @param output number of output tokens produced by the model
	 */
	public void recordUsage(long input, long output) {
	    inputTokens.addAndGet(input);
	    outputTokens.addAndGet(output);
	}

	/**
	 * @return the cumulative input token count across all transcriptions
	 */
	public long getInputTokens() {
	    return inputTokens.get();
	}

	/**
	 * @return the cumulative output token count across all transcriptions
	 */
	public long getOutputTokens() {
	    return outputTokens.get();
	}

}
