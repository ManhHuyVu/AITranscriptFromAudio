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

@RestController
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping("/api/v1/transcription")
    public ResponseEntity<?> transcribe(@RequestParam("audio") MultipartFile audio) {
        try {
            String transcript = transcriptionService.transcribe(audio);
            return ResponseEntity.ok(new TranscriptionResponse(transcript));
        } catch (Exception e) {
            System.err.println("Transcription failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            ErrorResponse error = new ErrorResponse(
                    Instant.now(), 502, "Bad Gateway", "Transcription request failed.", "/api/v1/transcription"
            );
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
        }
    }
}