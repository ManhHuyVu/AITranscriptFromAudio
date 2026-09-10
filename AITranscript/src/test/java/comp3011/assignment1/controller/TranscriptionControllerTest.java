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

@WebMvcTest(TranscriptionController.class)
class TranscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranscriptionService transcriptionService;

    private MockMultipartFile createDummyAudio() {
        return new MockMultipartFile(
                "audio",
                "recording.webm",
                "audio/webm",
                "dummy audio content".getBytes()
        );
    }

    @Test
    void transcribeReturnsTranscriptOnSuccess() throws Exception {
        when(transcriptionService.transcribe(any())).thenReturn("Hello world");

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcript").value("Hello world"));
    }

    @Test
    void transcribeReturnsTranscriptWithSpecialCharacters() throws Exception {
        when(transcriptionService.transcribe(any())).thenReturn("Hello! @#$%^&*()");

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcript").value("Hello! @#$%^&*()"));
    }

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

    @Test
    void transcribeReturns502WhenIOExceptionOccurs() throws Exception {
        when(transcriptionService.transcribe(any()))
                .thenThrow(new IOException("File read error"));

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void transcribeReturnsErrorResponseWithTimestamp() throws Exception {
        when(transcriptionService.transcribe(any()))
                .thenThrow(new RuntimeException("API error"));

        mockMvc.perform(multipart("/api/v1/transcription").file(createDummyAudio()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
