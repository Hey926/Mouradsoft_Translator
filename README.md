# Mouradsoft Translator

A native Android app that helps kids and grown-ups understand expressions. English is the language; **Child → Adult** and **Adult → Child** describe the direction of explanation, not anyone’s verified age.

The flow is deliberately **Welcome → Language and direction → Speak or type → Completion → Translation result**. A valid result waits behind **Finish**. **Redo** clears the session and navigation history.

## Run the Android app

1. Open this repository in Android Studio. Use its bundled JDK 21 (JDK 17 is the build minimum).
2. Install Android SDK Platform **36.1** and Build Tools **36.0.0** through SDK Manager. Let Android Studio create `local.properties` with your SDK location.
3. Sync Gradle and run the `app` configuration on an Android 8.0 / API 26 or newer device or emulator.

The checked-in wrapper uses Gradle 9.4.1, AGP 9.2.1 with built-in Kotlin, Compose compiler 2.2.10, and the stable Compose BOM 2026.02.01. These versions fit the available environment; this project does not require adopting every newest library. Target SDK is 36; revisit Play’s target requirements before distribution.

```powershell
# Windows; JAVA_HOME must point at your JDK, such as Android Studio's jbr directory.
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On macOS/Linux, use `./gradlew` (or `sh gradlew` if executable permission was not preserved). The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. No account, API key, or backend is needed for demo mode.

## Honest demo mode

Demo is the default. Input, completion, and result display **“Demo mode — sample translations only.”** The on-screen samples are bundled in string resources. Matching ignores case, curly versus straight apostrophes, extra whitespace, and final sentence punctuation. It never matches an arbitrary substring or silently substitutes a canned response for a failed live request.

Child → Adult samples include the test / “I’m cooked” example, the dance compliment, literal pasta cooking, and an ambiguous “I’m cooked” that asks for context. Adult → Child samples cover compromise, putting something on hold, and prioritizing homework. Unknown phrases stay on input with **“This phrase needs live AI. Try a sample phrase for now.”**

Demo translation itself is local. **Speech recognition is separate:** even in demo mode, a system speech service may process audio remotely. The app prefers the on-device recognizer when Android reports it available, and asks before using a service that may process audio remotely. It does not silently fall back from a failed on-device recognizer. Typing always remains available.

## Backend setup

`backend/` is a small Node.js **24 LTS** service using Node’s maintained HTTP and fetch APIs. There are no third-party runtime packages to install. The `TranslationProvider` contract keeps the AI provider behind the server; `OpenAITranslationProvider` implements the Responses API with strict structured output.

```powershell
cd backend
Copy-Item .env.example .env
npm start
```

On macOS/Linux, use `cp .env.example .env`. Default address: `http://127.0.0.1:8787`. `GET /health` confirms the server is running. By default, `POST /v1/translate` returns `503 live_disabled` and makes **no AI calls**.

To enable controlled live development, first complete the review in [docs/PRIVACY.md](docs/PRIVACY.md). Then set these **server-only** variables in ignored `backend/.env` or your server’s secret manager:

```dotenv
LIVE_TRANSLATION_ENABLED=true
CHILD_DATA_REVIEW_COMPLETED=true
OPENAI_API_KEY=your_server_secret
OPENAI_MODEL=your_reviewed_structured_output_model_id
```

The review flag is a deployment gate, not provider approval or proof of compliance. The model has no implicit default: choose a model available to your project that supports the Responses API and strict structured output, and evaluate it before launch. Do not put provider secrets into Android properties, BuildConfig, assets, resources, an APK, or version control. **Enabling and using live mode can incur charges. No billable provider calls were made during implementation.**

Build an opt-in live Android variant using only the public backend address:

```powershell
./gradlew.bat :app:assembleDebug -Ptranslator.liveEnabled=true -Ptranslator.backendUrl=http://10.0.2.2:8787/
```

For Android Studio, set the same `translator.liveEnabled` and `translator.backendUrl` properties locally (for example in your user Gradle properties), then sync. Avoid committing a local mode change. Return to demo by setting `translator.liveEnabled=false` or removing the override.

### Emulator and phone networking

- Android emulator: `10.0.2.2` addresses the development computer. Use the URL above while the backend runs locally.
- USB-connected phone: run `adb reverse tcp:8787 tcp:8787`, then build with `-Ptranslator.backendUrl=http://127.0.0.1:8787/`. This avoids a public port or LAN firewall change.
- Remote development or deployment: use a real **HTTPS** endpoint with a valid certificate. Release builds reject all cleartext URLs. Debug cleartext exceptions cover only `10.0.2.2`, `127.0.0.1`, and `localhost`; arbitrary LAN HTTP addresses are intentionally excluded.

The default backend binds to loopback. Public deployment requires a TLS reverse proxy and the launch work below. There is no shared secret embedded in the app masquerading as server authentication.

### API contract

`POST /v1/translate`, `Content-Type: application/json`:

```json
{"text":"We need to compromise.","language":"en","direction":"adult_to_child"}
```

A successful meaning response:

```json
{"translation":"We need to find a choice that works for both of us.","shortExplanation":"","needsMoreContext":false,"clarificationQuestion":""}
```

A clarification has empty translation/explanation, `needsMoreContext: true`, and a useful `clarificationQuestion`. The app remains on input until the words are edited. Both server and Android validate the contract. Refusals, empty output, invalid JSON, contradictory fields, excess output, and network errors cannot reach Completion.

Server controls: 1,000 UTF-16 input units (matching Android), English/direction validation, an 8 KiB body bound, 1,200 model output tokens, 2,000 translation / 600 explanation / 300 clarification characters, a 64 KiB provider envelope bound, a 25-second provider deadline and 27-second request deadline. Limits are 10 requests/minute per socket address, 60 globally, and 4 concurrent requests. Limits are per process. IP counters expire within approximately two minutes and are never logged. Forwarded client headers are not trusted; behind a proxy, clients share that proxy’s limit unless a carefully configured edge limiter is added.

## Architecture and design

- `ui/`: five screens, shared spacing/color/type/shape tokens, custom Canvas icons and original robot with four restrained poses. Scrollable content, reachable primary actions, system/keyboard insets, semantic headings, radio selection, and visible inline feedback.
- `session/SessionViewModel`: `StateFlow`, explicit mutually exclusive work states, coroutine jobs, and generation-based stale-result protection. Rotation keeps the same ViewModel and does not resubmit a request. No input or result is put in a saved-state bundle or persistent history.
- `ui/TranslatorApp`: Compose Navigation host driven by the in-memory session. A single host destination is retained; Android Back follows the explicit session flow. Restored framework routes cannot render an old result without matching in-memory state. Process death starts safely at Welcome.
- `speech/SpeechController`: screen-owned Android recognizer, main-thread calls, version guards, per-recognition callback generations, bounded listening, and resource release on navigation/backgrounding. Partial words replace the current transcript; they never repeatedly append. Existing text is preserved. Editing cancels recognition; speech never auto-submits.
- `data/`: small repository, `TranslationService`, labeled demo catalog, and cancellable OkHttp live adapter. Network callbacks run away from the main thread. No HTTP logging or disk cache.
- `backend/`: request validation, provider interface, bounded output validation, timeouts, rate limiting, and sanitized error codes.

All fixed app copy and demo text are in `strings.xml`; live translations and clarification questions are dynamic service content. The robot and launcher icon are authored locally. There are no remote artwork URLs, paid assets, ads, analytics, accounts, contact/location permissions, or persistent translation history.

## Verification

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd backend
npm test
npm run check
```

Tests use provider doubles and an injectable fake HTTP transport; they do not use AI-provider credentials. Local Compose tests use Robolectric API 34 and exercise the actual screens. They are simulated Android interactions, not physical-device screenshots.

See [docs/QA.md](docs/QA.md) for the verification record and physical-device checklist. Microphone hardware, actual installed speech services, real TalkBack behavior, OEM keyboard/insets, and paid live AI quality need real-device or explicitly authorized live verification.

## Before public use

This is a runnable prototype, **not a declaration of child-app compliance**. Complete the child-data/provider review, consent and retention design, applicable age assurance, privacy notices, Play Families/Data safety declarations, age-appropriate safety evaluation, accessibility/device tests, TLS deployment, abuse/spend controls, and operational procedures described in [docs/PRIVACY.md](docs/PRIVACY.md). No service has been publicly deployed.

Implementation references checked through the OpenAI Docs skill and official Android/Node documentation:

- [Android built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) and [AGP 9.2](https://developer.android.com/build/releases/agp-9-2-0-release-notes).
- [SpeechRecognizer lifecycle and on-device APIs](https://developer.android.com/reference/android/speech/SpeechRecognizer).
- [OpenAI structured output](https://developers.openai.com/api/docs/guides/structured-outputs), [under-18 guidance](https://developers.openai.com/api/docs/guides/safety-checks/under-18-api-guidance), and [data controls](https://developers.openai.com/api/docs/guides/your-data).
- [Node.js supported releases](https://nodejs.org/en/about/previous-releases).
