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

@Service
public class TranscriptionService {

    private final RestClient restClient;
    private final TokenUsageService tokenUsageService;

    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;

    public TranscriptionService(TokenUsageService tokenUsageService) {
    	RequestConfig requestConfig = RequestConfig.custom()
    	        .setConnectTimeout(10, TimeUnit.SECONDS)
    	        .setResponseTimeout(60, TimeUnit.SECONDS)
    	        .build();

        var httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                .build();

        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1/audio/transcriptions")
                .requestFactory(requestFactory)
                .build();

        this.tokenUsageService = tokenUsageService;
    }

    public String transcribe(MultipartFile audioFile) throws IOException {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not set on the environment.");
        }

        ByteArrayResource fileResource = new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() {
                return audioFile.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("model", "gpt-4o-mini-transcribe");

        Map<String, Object> response = restClient.post()
                .header("Authorization", "Bearer " + openaiApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);

        Object text = response.get("text");

        if (!(text instanceof String transcript) || transcript.isBlank()) {
            throw new IllegalStateException(
                "OpenAI response does not contain a valid transcription"
            );
        }
        
        recordTokenUsage(response);

        return transcript;
    }

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

    private long extractLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException(
                "Expected numeric token usage value but got: " + value
        );
    }
}