package comp3011.assignment1.service;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.ProxySelector;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

/**
 * Service responsible for sending audio files to the OpenAI Whisper API
 * and returning the text transcription.
 *
 * Dependencies:
 *   {@link org.apache.hc.client5.http.impl.classic.HttpClientBuilder} - Builds the underlying
 *       HTTP client with proxy and timeout support.
 *   {@link SystemDefaultRoutePlanner} - Routes requests through the JVM's default proxy
 *       selector so that {@code JAVA_TOOL_OPTIONS} proxy settings (e.g. {@code -Dhttps.proxyHost})
 *       are honoured in cloud/assessment environments like Titan.
 *   {@link RestClient} - Spring's lightweight HTTP client used to POST multipart form data
 *       to the OpenAI API.
 */
@Service
public class TranscriptionService {

    private RestClient restClient;
    private final TokenUsageService tokenUsageService;

    /**
     * The OpenAI API key read from the {@code OPENAI_API_KEY} environment variable at runtime.
     * Defaults to an empty string if not set; the {@link #transcribe} method validates this
     * before making any API call.
     */
    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;

    /**
     * The base URL for the transcription API endpoint.
     * Defaults to OpenAI's direct URL; overridden to OpenRouter in application-local.properties.
     */
    @Value("${OPENAI_BASE_URL:https://api.openai.com/v1/audio/transcriptions}")
    private String openaiBaseUrl;

    /**
     * Constructs the TranscriptionService and configures the RestClient with a tailored
     * Apache HttpClient.
     *
     * Constructor injection: is used so Spring manages the dependency lifecycle
     * and the class is testable (the {@link TokenUsageService} can be replaced with a mock).
     *
     * @param tokenUsageService the shared service that accumulates token usage counters
     */
    public TranscriptionService(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    /**
     * Initialises the RestClient after Spring has injected the @Value fields
     * (OPENAI_API_KEY and OPENAI_BASE_URL). This is needed because 
     * @Value fields are not available in the constructor.
     */
    @PostConstruct
    public void init() {
        // Timeout configuration:
        //   connectTimeout - max time to establish a TCP connection to OpenAI (10 s).
        //   responseTimeout - max time to wait for the full response body (60 s).
        //     OpenAI's transcription endpoint can be slow for longer audio; 
    	RequestConfig requestConfig = RequestConfig.custom()
    	        .setConnectTimeout(10, TimeUnit.SECONDS)
    	    	// the original 15 s was too short and caused "Network is unreachable" / timeout errors.
    	        .setResponseTimeout(60, TimeUnit.SECONDS)
    	        .build();

        // SystemDefaultRoutePlanner reads the JVM-wide proxy settings from
        // This is critical in the Titan assessment environment because traffic is routed
        // through a corporate proxy: without it, the HttpClient ignores JAVA_TOOL_OPTIONS
        // and the connection fails with "Network is unreachable".
        var httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                .build();

        // HttpComponentsClientHttpRequestFactory bridges Apache HttpClient 5 into
        // Spring's RestClient API so we can use Spring's fluent request building.
        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .baseUrl(openaiBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Sends the given audio file to the OpenAI {@code /v1/audio/transcriptions} endpoint
     * using the {@code gpt-4o-mini-transcribe} model and returns the transcribed text.
     *
     * After a successful transcription, the OpenAI response's {@code usage} object is
     * extracted and recorded via {@link TokenUsageService}.
     *
     * @param audioFile the multipart audio file uploaded from the client (webm, mp3, etc.)
     * @return the plain-text transcription produced by the model
     * @throws IllegalStateException if the API key is missing, or the response is malformed
     * @throws IOException if reading the file bytes fails
     */
    public String transcribe(MultipartFile audioFile) throws IOException {
        // Guard: refuse to call the API without a valid key.
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not set on the environment.");
        }

        // ByteArrayResource wraps raw bytes as a Spring Resource.
        // We override getFilename() so the multipart Content-Disposition header
        // carries the original filename (e.g. "recording.webm"), which OpenAI
        // uses to infer the audio codec when no explicit format is specified.
        ByteArrayResource fileResource = new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() {
                return audioFile.getOriginalFilename();
            }
        };

        // Build the multipart/form-data body that the OpenAI transcription API expects.
        //   "file"  - the audio binary
        //   "model" - must be "gpt-4o-mini-transcribe" per assignment spec
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("model", "gpt-4o-mini-transcribe");

        // POST the request with Bearer token auth.
        // .body(Map.class) deserialises the JSON response into a raw Map;
        // Jackson handles nested objects as Map<String, Object> automatically.
        Map<String, Object> response = restClient.post()
                .header("Authorization", "Bearer " + openaiApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);

        // The OpenAI transcription response contains a top-level "text" field.
        Object text = response.get("text");

        // Defensive check: if the response is missing "text" or it is blank,
        // surface a clear error rather than returning empty content to the UI.
        if (!(text instanceof String transcript) || transcript.isBlank()) {
            throw new IllegalStateException(
                "OpenAI response does not contain a valid transcription"
            );
        }
        
        // Record input/output token counts so the /api/v1/global/stats endpoint
        // can report cumulative usage across all transcription requests.
        recordTokenUsage(response);

        return transcript;
    }

    /**
     * Extracts the {@code usage} object from the OpenAI response and delegates to
     * {@link TokenUsageService#recordUsage(long, long)}.
     *
     * @param response the raw JSON response from OpenAI, parsed as a Map
     * @throws IllegalStateException if the response lacks a {@code usage} map
     */
    private void recordTokenUsage(Map<String, Object> response) {
        Object usage = response.get("usage");
        if (usage instanceof Map<?, ?> usageMap) {
            long input = extractLong(usageMap.get("input_tokens"));
            long output = extractLong(usageMap.get("output_tokens"));
            tokenUsageService.recordUsage(input, output);
        }
        else {
            throw new IllegalStateException(
                    "OpenAI response does not contain usage information"
            );
        }
    }

    /**
     * Safely converts a JSON numeric value (Integer, Long, Double) to a {@code long}.
     *
     * @param value the raw value from the JSON map (may be null or non-numeric)
     * @return the value as a {@code long}
     * @throws IllegalStateException if the value is null or not a {@link Number}
     */
    private long extractLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException(
                "Expected numeric token usage value but got: " + value
        );
    }
}
