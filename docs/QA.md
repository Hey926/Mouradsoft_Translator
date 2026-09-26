# Verification and manual checklist

## Automated coverage

- Session unit tests: deliberate direction selection; exact five stages; Finish gate; empty/whitespace input; both directions; duplicate prevention; failures/retry; clarification requiring an edit; invalid results; cancellation on editing/back/Redo; stale non-cooperative responses; state reset; transcript replacement and typed-text preservation; character limits; timeout and exceptions.
- HTTP client tests: structured request/response, no provider credential header, strict malformed/empty/contradictory validation, HTTPS restrictions, response bounds, error mapping and cancellation.
- Local Compose tests: visible five-screen sequence, no translation on completion, Redo and Android Back, rotation on input/completion, demo labeling, unsupported phrases, clarification, bundled samples and literal cooking.
- Backend tests: both directions and validation boundaries; disabled cloud; success/clarification; invalid/chunked/oversized bodies; malformed and empty output; safe errors; timeouts; disconnect cancellation; rate/concurrency limiting; provider request construction, refusal, incomplete output, oversized envelope and cancellation. All provider calls are faked.

Verified: debug APK assembled; all 21 Android unit/UI tests passed (12 ViewModel, 5 service, 4 Compose/Robolectric); Android lint passed with 0 errors and 15 dependency/target advisory warnings; all 15 Node backend tests and `npm run check` passed. A local backend startup smoke check returned `{"status":"ok","liveEnabled":false}` from `/health` and HTTP 503 from `/v1/translate` without contacting a provider. The temporary test server was stopped afterward.

Robolectric exercises app interactions on simulated Android; no emulator or physical device is attached. It cannot establish physical speech, actual accessibility behavior, or live provider quality. The focused fake transport avoids local network calls from Robolectric.

## Physical device and emulator checklist

Use synthetic example phrases throughout. Test API 26, API 31+, and a current Android version on more than one OEM when possible.

### Navigation and state

- [ ] Start → select a direction → Continue → type → Translate → Completion → Finish → Result.
- [ ] Continue stays disabled until a direction is deliberately chosen; English has no misleading dropdown.
- [ ] Completion never displays the actual translation or advances on a timer.
- [ ] Redo empties text, speech, errors, clarification, result and consents; English resets, direction clears. Android Back cannot show the old result.
- [ ] Back during a slow request invalidates it; a late response never changes a new session.
- [ ] Rotate on each screen and while translating. No duplicate network call and no lost reviewed input.
- [ ] Kill the process while backgrounded and restore: Welcome appears, with no old sensitive text.

### Microphone

- [ ] No permission request at launch, Start, language selection, or typing. Only the mic action requests it.
- [ ] First permission grant starts listening after the activity resumes. Denial shows an inline typing alternative.
- [ ] Permanent denial offers working app settings. Grant there and return; tapping the mic works.
- [ ] On-device recognition is selected on a compatible device with an English model. Missing model produces a useful error; no silent remote fallback.
- [ ] On a device with only remote-capable recognition, disclosure appears before starting; declining leaves typing available. Accepting uses the service honestly labeled as potentially remote.
- [ ] Test missing service, no speech, silence timeout, busy microphone, network failure, service failure and permission revocation.
- [ ] Partial speech replaces the earlier partial, preserving existing typed text; final speech does not duplicate words. Stop waits for final words; Cancel keeps the latest text.
- [ ] Editing while listening stops recognition and keeps the edited words. Translate stops recognition and submits only the reviewed field.
- [ ] Background, screen off, Back and Redo stop/release recognition. There is no continuing microphone indicator or background recording.

### Layout and accessibility

- [ ] 320dp-wide phone, short screen, landscape, tablet, display cutouts, gesture navigation, and three-button navigation.
- [ ] Font scale 100%, 150%, and 200%: headings wrap, full directions stay readable, buttons grow, long text scrolls naturally.
- [ ] Keyboard open: field/cursor and Translate remain reachable; system/IME insets do not double-pad or overlap content.
- [ ] Long 1,000-character input and long live translation: no clipped meaning, destructive truncation, or unreadably small type.
- [ ] TalkBack: headings, buttons, direction radio state, text entry, counter, listening/loading and inline errors are understandable. Robot strokes and decorative icons are silent.
- [ ] Color-vision and contrast review: selected direction is also indicated by radio state, border and “Selected.” Check text and action contrast.
- [ ] Result tail clearly points to the explaining robot. Motion is short and purposeful; system reduced animation settings are respected.

### Live service (explicit authorization and completed review required)

- [ ] Configure actual HTTPS server and approved provider project/model. Do not use production child data as test input.
- [ ] Confirm live disclosure before first submission, no demo label, and no fallback to sample results on failure.
- [ ] Check literal versus slang “cooked,” dance compliment, compromise, on-hold, negation, uncertainty, unknown slang and additional context.
- [ ] Slow network, offline, timeout, 429, refusal, invalid backend output and unavailable backend all preserve input and remain on the input screen.
- [ ] Inspect deployed logs/settings without logging test content: no body logging, secrets, translation history or recordings. Confirm real retention controls across all vendors.
