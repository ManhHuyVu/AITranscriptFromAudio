package comp3011.assignment1.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;

//AtomicLong matters here because multiple virtual threads hit this at once, 
//one per concurrent transcription request. A plain long field can lose updates under concurrent writes. 
//AtomicLong guarantees each increment is safe.

//Where did I know this: Expand research on Kai's method on Christian's practical Week 2

@Service
public class TokenUsageService {

	// This class is made to get number of token used and generated with gpt
	
	private final AtomicLong inputTokens = new AtomicLong(0);
	private final AtomicLong outputTokens = new AtomicLong(0);

	public void recordUsage(long input, long output) {
	    inputTokens.addAndGet(input);
	    outputTokens.addAndGet(output);
	}

	public long getInputTokens() {
	    return inputTokens.get();
	}

	public long getOutputTokens() {
	    return outputTokens.get();
	}

}