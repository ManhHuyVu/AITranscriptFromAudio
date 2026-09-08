const recordButton = document.getElementById('recordButton');
const stopButton = document.getElementById('stopButton');
const statusText = document.getElementById('recordingStatus');
const transcriptOutput = document.getElementById('transcriptOutput');

let mediaRecorder;
let audioChunks = [];

recordButton.addEventListener('click', async () => {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];

        mediaRecorder.ondataavailable = (event) => {
            audioChunks.push(event.data);
        };

        mediaRecorder.onstop = async () => {
            stream.getTracks().forEach(track => track.stop());
            const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
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
    mediaRecorder.stop();
    stopButton.classList.add('d-none');
    statusText.textContent = 'Transcribing.';
});

async function sendForTranscription(audioBlob) {
    const formData = new FormData();
    formData.append('audio', audioBlob, 'recording.webm');

    try {
        const response = await fetch('/api/v1/transcription', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error('Server returned ' + response.status);
        }

        const data = await response.json();
        transcriptOutput.value = data.transcript;
        statusText.textContent = 'Done. Ready for another recording.';
    } catch (error) {
        statusText.textContent = 'Transcription failed. Try again.';
    } finally {
        recordButton.classList.remove('d-none');
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