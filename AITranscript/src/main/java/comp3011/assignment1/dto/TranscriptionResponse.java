package comp3011.assignment1.dto;

/**
 * Response DTO for a successful transcription request.
 *
 * <p>Serialised to JSON as: {@code {"transcript": "..."}}</p>
 *
 * <p>Uses a Java record (introduced in Java 16+) which auto-generates
 * the constructor, getters, {@code equals}, {@code hashCode}, and {@code toString}.
 * Records are immutable and concise — ideal for DTOs that carry data
 * with no business logic.</p>
 *
 * @param transcript the transcribed text from the OpenAI API
 */
public record TranscriptionResponse(String transcript) {}
