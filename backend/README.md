# Food Memory AI demo service

This small Node.js service keeps the Gemini API key outside the Android APK. It accepts a dish photo for structured analysis and a minimal, data-derived taste profile for recommendations. It does not save photos or request data.

## Run locally

Use Node.js 18 or newer. Add your key to `backend/.env` (that file is git-ignored):

```sh
node backend/server.mjs
```

The server reads `backend/.env` automatically. Environment variables already set in the shell take precedence. `backend/.env.example` documents the available settings. Keep this key on the server; never add it to the Android project or APK.

The Android emulator reaches the host machine at `http://10.0.2.2:8080/`. To use another server, set the Gradle property `foodMemoryApiBaseUrl` to its base URL. The Android release build disables cleartext traffic; a published build must use an HTTPS deployment.

The image is sent to Google's Gemini API only after the user taps **Analizar con IA**. Recommendations send only highly rated dish names and restaurants; private notes, companions, and photos stay on-device. The server does not log or persist request data. `GET /health` reports whether a key was loaded without revealing it.
