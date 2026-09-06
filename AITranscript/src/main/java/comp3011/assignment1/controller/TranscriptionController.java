package comp3011.assignment1.controller;

import comp3011.assignment1.dto.TranscriptionResponse;
import comp3011.assignment1.service.TranscriptionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class TranscriptionController {

private final TranscriptionService transcriptionService;

public TranscriptionController(TranscriptionService transcriptionService) {
    this.transcriptionService = transcriptionService;
}

@PostMapping("/api/v1/transcription")
public TranscriptionResponse transcribe(@RequestParam("audio") MultipartFile audio) throws IOException {
    String transcript = transcriptionService.transcribe(audio);
    return new TranscriptionResponse(transcript);
}

}