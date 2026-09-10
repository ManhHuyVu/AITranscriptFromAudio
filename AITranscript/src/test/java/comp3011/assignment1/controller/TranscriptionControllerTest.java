package comp3011.assignment1.controller;

import comp3011.assignment1.service.TranscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link TranscriptionController}.
 *
 * <p><b>Strategy:</b> The real {@link TranscriptionService} is replaced with a
 * {@code @MockitoBean} so we never call the OpenAI API during unit tests.
 * This lets us test the controller's request handling, response formatting,
 * and error mapping in isolation.</p>
 *
 * <p><b>Tests cover:</b></p>
 * <ul>
 *   <li>Successful transcription (200 OK with transcript field)</li>
 *   <li>Special characters in transcript output</li>
 *   <li>Service exceptions → 502 Bad Gateway with error details</li>
 *   <li>IOException handling</li>
 *   <li>Response structure (timestamp field in errors)</li>
 * </ul>
 */
@WebMvcTest(TranscriptionController.class)
class TranscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Mocked transcription service — we control what it returns or throws. */
    @MockitoBean
    private TranscriptionService transcriptionService;

    /**
     * Helper to create a dummy multipart audio file for test requests.
     *
     * <p>Uses {@link MockMultipartFile} which implements the {@code MultipartFile}
     * interface without needing a real file on disk. The content is arbitrary
     * bytes — we only need the controller to accept the request.</p>
     *
     * @return a mock audio file named "recording.webm" with content type "audio/webm"
     */
    private MockMultipartFile createDummyAudio() {
        return new MockMultipartFile(
                "audio",
                "recording.webm",
                "audio/webm",
                "dummy audio content".getBytes()
        );
    }

    /**
     * Happy path: service returns a transcript, controller wraps it in
     * {@code TranscriptionResponse} and returns HTTP 200.
     */
    @Test
    void transcribeReturnsTranscriptOnSuccess() throws Exception {
        when(transcriptionService.transcribe(any())).thenReturn("Hello world");

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcript").value("Hello world"));
    }

    /**
     * Verifies that special characters and symbols in the transcript
     * are preserved in the JSON response (not escaped or truncated).
     */
    @Test
    void transcribeReturnsTranscriptWithSpecialCharacters() throws Exception {
        when(transcriptionService.transcribe(any())).thenReturn("Hello! @#$%^&*()");

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcript").value("Hello! @#$%^&*()"));
    }

    /**
     * When the OpenAI API call fails (timeout, network error, etc.), the
     * controller returns HTTP 502 with the actual error message from the
     * exception. This is critical for Titan diagnostics — the error message
     * is displayed on the page so the assessment system can capture it.
     */
    @Test
    void transcribeReturns502WhenServiceThrowsException() throws Exception {
        when(transcriptionService.transcribe(any()))
                .thenThrow(new RuntimeException("OpenAI timeout"));

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("OpenAI timeout"))
                .andExpect(jsonPath("$.path").value("/api/v1/transcription"));
    }

    /**
     * IOExceptions (e.g. file read errors) are also caught and returned as 502.
     * This ensures the client always gets a structured JSON error, never a raw
     * Spring error page.
     */
    @Test
    void transcribeReturns502WhenIOExceptionOccurs() throws Exception {
        when(transcriptionService.transcribe(any()))
                .thenThrow(new IOException("File read error"));

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    /**
     * Structural test: every error response must include a {@code timestamp}
     * field so Titan can log when the failure occurred.
     */
    @Test
    void transcribeReturnsErrorResponseWithTimestamp() throws Exception {
        when(transcriptionService.transcribe(any()))
                .thenThrow(new RuntimeException("API error"));

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
