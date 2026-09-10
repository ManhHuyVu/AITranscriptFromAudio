package comp3011.assignment1.controller;

import comp3011.assignment1.dto.ErrorResponse;
import comp3011.assignment1.dto.TranscriptionResponse;
import comp3011.assignment1.service.TranscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/**
 * REST controller that exposes the {@code POST /api/v1/transcription} endpoint.
 *
 * <p>Accepts a multipart audio file from the client, delegates to
 * {@link TranscriptionService} for the OpenAI API call, and returns either
 * a {@link TranscriptionResponse} (200 OK) or an {@link ErrorResponse} (502 Bad Gateway).</p>
 */
@RestController
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    /**
     * Constructor-injected dependency. Spring creates a single instance
     * (singleton scope) and wires the service automatically.
     */
    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    /**
     * Receives an audio file upload and returns the transcription.
     *
     * <p>The {@code @RequestParam("audio")} annotation tells Spring to extract
     * the part named {@code "audio"} from the multipart request body
     * (matching the {@code formData.append('audio', ...)} call in recorder.js).</p>
     *
     * <p><b>Error handling:</b> Any exception from the service layer (network errors,
     * missing API key, bad response format) is caught here and translated into an
     * {@link ErrorResponse} with HTTP 502, because the failure originates from the
     * upstream OpenAI API — not from the client's request.</p>
     *
     * @param audio the uploaded audio file (multipart, any format supported by OpenAI)
     * @return 200 with {@link TranscriptionResponse} on success, or 502 with {@link ErrorResponse}
     */
    @PostMapping("/api/v1/transcription")
    public ResponseEntity<?> transcribe(@RequestParam("audio") MultipartFile audio) {
        try {
            String transcript = transcriptionService.transcribe(audio);
            return ResponseEntity.ok(new TranscriptionResponse(transcript));
        } catch (Exception e) {
            // Log the exception class and message for server-side debugging.
            // We intentionally do NOT log the API key or full request body.
            System.err.println("Transcription failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());

            // Surface the actual error reason to the frontend so Titan can
            // capture meaningful failure messages on screen.
            String detail = e.getMessage() != null ? e.getMessage() : "Transcription request failed.";
            ErrorResponse error = new ErrorResponse(
                    Instant.now(), 502, "Bad Gateway", detail, "/api/v1/transcription"
            );
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
        }
    }
}
