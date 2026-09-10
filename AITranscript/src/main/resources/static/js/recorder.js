/**
 * recorder.js — Client-side audio recording and transcription upload.
 *
 * <p>This module handles two input paths:</p>
 * <ol>
 *   <li><b>Live recording:</b> Uses the Web Audio API (MediaRecorder) to capture
 *       microphone input, then POSTs the audio blob to the Spring Boot backend.</li>
 *   <li><b>File upload:</b> Reads a user-selected audio file and sends it directly.</li>
 * </ol>
 *
 * <p><b>Key Web APIs used:</b></p>
 * <ul>
 *   <li>{@link navigator.mediaDevices.getUserMedia} — requests microphone access and returns
 *       a MediaStream that feeds into the MediaRecorder.</li>
 *   <li>{@link MediaRecorder} — encodes the audio stream into webm chunks via the browser's
 *       built-in codecs. No external libraries needed.</li>
 *   <li>{@link Blob} — combines recorded chunks into a single audio blob for upload.</li>
 *   <li>{@link FormData} — wraps the blob as multipart/form-data for the POST request.</li>
 * </ul>
 */

// ── DOM element references ──────────────────────────────────────────────
const recordButton = document.getElementById('recordButton');
const stopButton = document.getElementById('stopButton');
const statusText = document.getElementById('recordingStatus');
const transcriptOutput = document.getElementById('transcriptOutput');

/** The MediaRecorder instance; null until recording starts. */
let mediaRecorder;

/** Accumulated audio data chunks from the MediaRecorder. */
let audioChunks = [];

// ── Record button handler ───────────────────────────────────────────────
recordButton.addEventListener('click', async () => {
    try {
		// Clear any previous transcript while starting a new recording.
		transcriptOutput.value = '';

        // Request microphone permission from the browser.
        // Returns a MediaStream containing audio tracks.
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

        // MediaRecorder encodes the stream into webm format by default.
        // The recorded chunks will be concatenated into a single Blob later.
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];

		// ondataavailable fires periodically (or once on stop) with a
		// Blob chunk of encoded audio data. We collect all chunks.
		mediaRecorder.ondataavailable = (event) => {
		    if (event.data && event.data.size > 0) {
		    	audioChunks.push(event.data);
			}
		};
		
		// onstop fires after mediaRecorder.stop() is called.
		// We assemble the chunks, release the microphone, and upload.
        mediaRecorder.onstop = async () => {
            // Release the microphone — stops the red recording indicator
            // in the browser tab and frees the hardware resource.
            stream.getTracks().forEach(track => track.stop());

            // Combine all recorded chunks into a single Blob with the
            // correct MIME type (audio/webm). This Blob is what gets
            // uploaded to the backend.
            const audioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType });
            await sendForTranscription(audioBlob);
        };

        // Start recording. Data will be delivered via ondataavailable.
        mediaRecorder.start();

        // Update the UI to show recording is in progress.
        // Titan checks this text to confirm the "recording started" state.
        statusText.textContent = 'Recording. Speak now.';

        // Toggle button visibility: hide record, show stop.
        // 'd-none' is a Bootstrap utility class that sets display:none.
        recordButton.classList.add('d-none');
        stopButton.classList.remove('d-none');
    } catch (error) {
        // getUserMedia rejects if the user denies microphone permission
        // or if no microphone is available.
        statusText.textContent = 'Microphone access denied or unavailable.';
    }
});

// ── Stop button handler ─────────────────────────────────────────────────
stopButton.addEventListener('click', () => {
	// Guard: only act if we're actually recording.
	if (!mediaRecorder || mediaRecorder.state !== 'recording') {
	        return;
	    }

	    // Disable the stop button to prevent double-clicks.
	    stopButton.disabled = true;

	    // This triggers the onstop callback above.
	    mediaRecorder.stop();

	    // Titan checks this text to confirm "recording stopped" state.
	    statusText.textContent = 'Transcribing...';
});

// ── Transcription upload function ───────────────────────────────────────

/**
 * Sends an audio Blob or File to the backend transcription endpoint.
 *
 * @param {Blob|File} audioBlob - the audio data to transcribe
 * @returns {Promise<void>} resolves when the UI is updated (success or error)
 *
 * <p><b>Backend contract:</b> The Spring controller expects a multipart form
 * with a part named "audio" (matching the @RequestParam("audio") annotation).</p>
 *
 * <p><b>Error handling:</b> We always parse the JSON response body first, then
 * check response.ok. This lets us surface the server's error message
 * (e.g. "OpenAI timeout") to the user on screen — which Titan captures.</p>
 */
async function sendForTranscription(audioBlob) {
    const formData = new FormData();
    // The third argument sets the filename in the Content-Disposition header.
    formData.append('audio', audioBlob, 'recording.webm');

    try {
        const response = await fetch('/api/v1/transcription', {
            method: 'POST',
            body: formData
        });

        // Always parse JSON — both success and error responses are JSON.
        const data = await response.json();

        // Check for HTTP errors AFTER parsing so we can read the error detail.
        if (!response.ok) {
            const detail = data.message || data.error || 'Server returned ' + response.status;
            throw new Error(detail);
        }

        // Success: display the transcript in the textarea.
        transcriptOutput.value = data.transcript;
        statusText.textContent = 'Done. Ready for another recording.';
    } catch (error) {
        // Network errors, JSON parse errors, or the explicit throw above
        // all land here. The error message is shown on screen for Titan
        // to capture and for the user to debug.
        statusText.textContent = 'Transcription failed: ' + error.message;
    } finally {
        // Restore the UI to the "ready to record" state regardless of outcome.
        recordButton.classList.remove('d-none');
		stopButton.disabled = false;
    }
}

// ── File upload handler (alternative to live recording) ──────────────────

const audioFileInput = document.getElementById('audioFileInput');

/**
 * Handles the file input change event — when the user selects an audio file
 * via the file picker, it is immediately sent for transcription.
 *
 * <p>This is an alternative to live recording, useful for testing with
 * pre-recorded audio files. The same {@link sendForTranscription} function
 * is reused, so the backend handling is identical.</p>
 */
audioFileInput.addEventListener('change', async () => {
    const file = audioFileInput.files[0];
    if (!file) {
        return;
    }
    statusText.textContent = 'Transcribing uploaded file.';
    recordButton.classList.add('d-none');
    await sendForTranscription(file);
    // Reset the file input so the same file can be re-selected if needed.
    audioFileInput.value = '';
})
