# Verification — October 12, 2026

## Completed

- `assembleDebug`: passed; installable, development-signed APK produced.
- `testDebugUnitTest`: **25 tests passed**, zero failures/errors/skips. Android-dependent tests ran at API 30 and API 35.
  - 4 activation-code grammar/length/confirmation-code tests.
  - 6 intent and companion availability tests (3 scenarios on each Android API).
  - 14 UI/state/scan-result tests (7 scenarios on each Android API).
  - 1 generated QR image → ZXing decode → exact activation credential round trip.
- `lintDebug`: passed with no errors. Two informational dependency-version warnings remain for deliberately pinned AndroidX Core/AppCompat versions; there are no layout/backup warnings.
- APK signing verified with Android SDK `apksigner` (APK Signature Scheme v2).
- Final APK manifest inspected: camera is its only device access permission. AndroidX also declares an internal signature permission for non-exported dynamic receivers. No internet, Bluetooth, telephony, or eSIM permissions.
- Installed and launched on an isolated Android 15/API 35 x86_64 emulator.
- Camera permission prompt exercised; embedded scanner opened the emulated camera without a crash. Final scanner uses the app's non-exported `ScanActivity` and `FLAG_SECURE`.
- Manual entry/review/confirmation controls exercised with the synthetic code `LPA:1$smdp.example.com$TEST-123`.
- A helper handoff opened the supplied **unmodified** Pixel Watch app 5.0.0.958206140. Android activity inspection showed the exact percent-encoded activation code, `dest=esim_download`, and the explicit original app component.
- With no paired watch, the original app displayed **Your watch, your way / Set up watch**. This verifies delivery to the app, not acceptance of the hidden download screen after pairing.
- Unit checks verify missing/disabled companion rejection, no browser or phone-installer fallback, manual/scanned-code confirmation, cancellation, edits invalidating review, and clearing credentials after handoff/backgrounding/recreation.

The emulator had no accounts, no paired watch, and Wi-Fi/mobile data disabled during original-app handoff testing. All activation credentials were synthetic and nonfunctional. No actual SIM/eSIM or carrier account was touched.

## Subsequent manual testing

The user tested the helper on an unlocked Pixel Watch 2 LTE with a separate travel eSIM. The watch reported activation success and its installed ICCID matched the purchased profile. With phone Bluetooth disconnected and watch Wi-Fi disabled, the watch reported LTE, out of service, and a suspended network state. The provider's instructed APN matched the selected APN. These observations support successful profile delivery, but do not establish working cellular service or broad compatibility. Registration/roaming troubleshooting remains open. This was user-reported testing, not a device session observed by the automated tests.

## Still requires a phone and LTE watch

1. Pair the intended watch with the original companion, then test a distinct disposable watch plan's valid QR code through this helper.
2. Confirm that the hidden destination actually reaches the download UI with a paired watch and that the watch's provisioning service accepts the profile. Record the watch EID and resulting ICCID.
3. Compare phone profile ICCIDs, enabled state, default subscriptions, and cellular service before and after; verify no phone provisioning screen is opened.
4. Test an unsupported phone carrier, no phone SIM, dual SIM, a different-carrier watch plan, an already-installed watch profile, and multiple paired watches.
5. Test disconnecting/switching the selected watch after review. The helper cannot bind to or verify a specific watch; the original app selects its current watch at handoff.
6. Verify provider confirmation-code behavior, consumed QR codes, capacity failures, reboot requirements, LTE registration, and the companion's mismatch prompts.

Do not interpret opening the original app, a successful activity result, or this test suite as proof of eSIM installation. The helper receives no provisioning result callback.
