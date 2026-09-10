const recordButton = document.getElementById('recordButton');
const stopButton = document.getElementById('stopButton');
const statusText = document.getElementById('recordingStatus');
const transcriptOutput = document.getElementById('transcriptOutput');

let mediaRecorder;
let audioChunks = [];

recordButton.addEventListener('click', async () => {
    try {
		transcriptOutput.value = '';
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];

		mediaRecorder.ondataavailable = (event) => {
		    if (event.data && event.data.size > 0) {
		    	audioChunks.push(event.data);
			}
		};
		
        mediaRecorder.onstop = async () => {
            stream.getTracks().forEach(track => track.stop());
            const audioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType });
            await sendForTranscription(audioBlob);
        };

        mediaRecorder.start();
        statusText.textContent = 'Recording. Speak now.';
        recordButton.classList.add('d-none');
        stopButton.classList.remove('d-none');
    } catch (error) {
        statusText.textContent = 'Microphone access denied or unavailable.';
    }
});

stopButton.addEventListener('click', () => {
	if (!mediaRecorder || mediaRecorder.state !== 'recording') {
	        return;
	    }

	    stopButton.disabled = true;
	    mediaRecorder.stop();
	    statusText.textContent = 'Transcribing...';
});

async function sendForTranscription(audioBlob) {
    const formData = new FormData();
    formData.append('audio', audioBlob, 'recording.webm');

    try {
        const response = await fetch('/api/v1/transcription', {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (!response.ok) {
            const detail = data.message || data.error || 'Server returned ' + response.status;
            throw new Error(detail);
        }

        transcriptOutput.value = data.transcript;
        statusText.textContent = 'Done. Ready for another recording.';
    } catch (error) {
        statusText.textContent = 'Transcription failed: ' + error.message;
    } finally {
        recordButton.classList.remove('d-none');
		stopButton.disabled = false;
    }
}

const audioFileInput = document.getElementById('audioFileInput');

audioFileInput.addEventListener('change', async () => {
    const file = audioFileInput.files[0];
    if (!file) {
        return;
    }
    statusText.textContent = 'Transcribing uploaded file.';
    recordButton.classList.add('d-none');
    await sendForTranscription(file);
    audioFileInput.value = '';
})