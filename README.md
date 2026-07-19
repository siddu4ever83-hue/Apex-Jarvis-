# JARVIS HUD — Android App (Kotlin + Jetpack Compose)

Holographic HUD-style voice assistant app, inspired by the Mark-XLIX dashboard
(waveform ring, dark cyan/red theme, log panel, mic button).

## What this is
- A native Android app (no Termux, no VNC, no PC needed to run it)
- Dark holographic dashboard with an animated waveform ring
- Voice input (Android's built-in speech recognizer)
- Text-to-speech replies
- Sends your voice text to an AI model via OpenRouter and speaks the reply
- Scrolling log panel showing the conversation

## Setting up API keys (all inside the app now — no code editing needed)
1. Install and open the app
2. Tap the ⚙ gear icon (top-right of the dashboard)
3. Tap each provider to expand it, fill in the key, and tap SAVE:
   - **OpenRouter** — API key (get free at https://openrouter.ai/keys) + pick a model from the list
   - **ElevenLabs** — API key (get at https://elevenlabs.io) + Voice ID. Leave blank to always use the phone's built-in voice. If ElevenLabs fails or its free limit runs out, the app automatically falls back to the phone's built-in text-to-speech — it never goes silent.
   - **NewsAPI** — API key (get free at https://newsapi.org/register) + country code (e.g. `in`)
4. Go back — the app is ready to use

Once NewsAPI is set, the app automatically reads out the day's top headlines when it opens,
shows them scrolling in a news ticker, then says "Cheppandi boss" and opens the mic for you.

## How to build the APK without a PC (GitHub Actions)
1. Create a new **public or private** GitHub repository (e.g. `JarvisHUD`)
2. Upload all these files/folders into that repo, keeping the same folder structure
   (you can do this from the GitHub website/app on your phone — use "Add file" → "Upload files")
3. Make sure the branch is named `main`
4. Go to the repo's **Actions** tab — a workflow called "Build APK" will run automatically
   (or click "Run workflow" manually if it doesn't start)
5. Wait for it to finish (green checkmark, a few minutes)
6. Click on the completed run → scroll down to **Artifacts** →
   download `JarvisHUD-debug-apk`
7. This downloads a `.zip` — open it, you'll find `app-debug.apk` inside
8. Transfer/download that APK to your phone and install it
   (allow "install from unknown sources" if asked)

## Notes
- This is a debug build (fine for personal use, not for Play Store distribution)
- Mic permission is requested on first launch — allow it
- Internet permission is used to call the AI model
