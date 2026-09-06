package comp3011.assignment1.dto;

public class TranscriptionResponse {
	
	private final String transcript;

	public TranscriptionResponse(String transcript) {
	    this.transcript = transcript;
	}

	public String transcript() {
	    return transcript;
	}
	
}
