package comp3011.assignment1.dto;

/**
 * Response DTO for a successful transcription request.
 *
 * Translate to JSON as: {@code {"transcript": "..."}}
 *
 * @param transcript the transcribed text from the OpenAI API
 */
public record TranscriptionResponse(String transcript) {}
