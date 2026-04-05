# Manual and scripted checks

Automated coverage is intentionally small; Telecom and OEM behavior still need eyes on hardware.

## Automated

- **Unit tests** (`./gradlew testDebugUnitTest`): `PhoneKeyMapping`, `CallStateHolder.clear`, `BroadcastActions` constants, `CallRecorder` path helpers.
- **Android instrumented** (`./gradlew connectedDebugAndroidTest`): package id smoke test.

## Telecom edge cases (manual)

Run these on a **physical device** with the app set as **default dialer** where applicable.

| Area | What to verify |
|------|----------------|
| **Multi-call** | Second incoming or outgoing while one call is active; **swap** from the in-call banner; hold/unhold; end one leg and confirm UI promotes the other. |
| **Rapid disconnect** | End call immediately after answer, or decline while UI is still animating; no crash, notification dismissed. |
| **Permissions** | Deny phone/microphone/contacts one at a time from system settings; app degrades (no crash); grant again and retry recording / contacts. |
| **Low memory** | With developer options, background the app under memory pressure during ringing; incoming UI and notification still recover when foregrounded. |

## Accessibility & display

- **TalkBack**: walk **Settings** rows, **dial pad** keys, **in-call** answer/decline/end; each control should have a single clear spoken label.
- **Largest font / display size**: open **Settings** and **in-call** with system font at maximum; lists should scroll rather than clip critical actions.

## Release note

Installing a build with a new **`applicationId`** (`com.zeno.zenoclassicdialer`) installs **alongside** older `com.zeno.bbclassicdialer` builds; uninstall the old package if you only want one icon.
