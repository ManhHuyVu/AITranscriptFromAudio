# AITranscriptFromAudio
Assignment for CaCP

# AI Assistance Disclosure

I used Claude, an AI assistant from Anthropic, throughout development of this assignment. Its role and my own work broke down as follows.

Code generation, co design backend, and explainning elements of code. Claude generated initial implementations for the backend, including AdminController, StatsController, TranscriptionController, TranscriptionService, TokenUsageService, and the response DTOs. I reviewed each piece, asked for explanations of unfamiliar concepts, such as virtual threads and Spring profiles, before accepting the code into the project.

My own work. I set up the project structure, resolved a JDK version issue, fixed a build misconfiguration that produced an invalid jar, diagnosed and corrected package and import errors as I integrated files, build front-end page using bootstrap template including eventlistener in main.js and recorder.js, and ran every submission against TITAN myself. When the transcription endpoint failed against TITAN, I worked through the diagnosis with Claude, identified the outbound timeout as too aggressive, added error handling, and verified the fix through local testing. along with the recorder.js file handling microphone capture and file upload

Design decisions. Choices such as using a single TranscriptionService instead of a client interface and mock split, the endpoint path and multipart field naming for the transcription request, and the token usage tracking approach were made by me

Third party template. The frontend is adapted from the Start Bootstrap Creative theme, MIT licensed, credited in the page footer and here in this write-up.