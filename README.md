# Watch eSIM Helper

## Overview
A standalone Android app that writes an eSIM (QR code or pasted text) to the pixel watch linked to the phone. You need to have **original Google Pixel Watch app** installed and your watch linked to your phone. This app does not replace or patch the original Google Pixel Watch app. 

## How is this app made?
This app is written entirely (100%) by Codex and I have not reviewed its code. Codex found the hidden configuration page in the original Google Pixel Watch app, likely reserved for Google Fi app, using https://github.com/morluto/rea. Fortunately, this path does not require any authentication as of now. Here's hoping Google doesn't change it!

## Testing
I manually tested it on a US-version Pixel Watch 2 LTE. It successfully writes all esims I gave it onto the watch. 
Due to the region I am in (China) and the restrictive carrier policy there I wasn't able to get internet to work on the watch. But at least the watch is able to connect to the carrier (China Telecom) before being suspended by carrier. 
Codex did not test it on an actual watch.

## Pitfalls
This is more of pitfalls from the Pixel Watch itself/carriers than this app. 
Pixel watches have restrictive roaming policies baked into its firmware; reverse engineering shows the esim home carrier's MCC & the current location MCC is divided into two buckets, US vs non-US, and if the buckets differ a limit will be applied to the connection. 
For anyone trying to use this app: your esim is not guaranteed to work, expect restrictions from cross-region roaming, your esim's home carrier and your local carrier. 


All contents below in this README are written by codex. 

## Install and use

Install `artifacts/watch-esim-helper-0.1.0-debug.apk` alongside the original Pixel Watch app. It uses its own package name, `io.github.watchesim.helper`, and is signed with a development key. You do **not** need to uninstall Pixel Watch.

Prebuilt packages in `artifacts/` are local delivery files and are not tracked in Git. From a fresh checkout, build the app using the instructions below; the APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

1. In the original Pixel Watch app, select and connect the intended LTE watch.
2. Open Watch eSIM Helper and scan your **separate watch plan** QR code, or paste its activation text.
3. Review the activation server and confirm the intended watch/plan.
4. Tap **Continue in Pixel Watch**, then complete setup there.

The helper cannot inspect the selected watch, pair it, or determine whether installation succeeded. The original app may display Google Fi wording. A replacement/transfer activation code for your phone's current line may affect that line at the carrier even though this helper never accesses phone SIMs. Use a distinct watch subscription.

## Experimental compatibility

The entry point was traced in the supplied Pixel Watch APK **5.0.0.958206140** (version code 331702839). Subsequent manual testing on a Pixel Watch 2 LTE reported successful profile installation with a matching ICCID; cellular connectivity remains unresolved. Other app versions, watch firmware, and provider profiles may behave differently. Opening an activity is not confirmation that the hidden destination was accepted or that an eSIM was installed.

The path accepts an activation code and sends a download request to the companion's current watch node. It does not run the normal carrier-selection screen. This app explicitly targets:

```
com.google.android.apps.wear.companion/
com.google.android.apps.wear.companion.core.application.RootActivity
```

with `ACTION_VIEW` and this URI, using `Uri.Builder` to encode parameters:

```
https://madeby.google.com/wear-companion/navigation
  ?dest=esim_download
  &activationCode=<scanned activation code>
  &carrier=Watch%20plan
```

It never falls back to a browser, phone eSIM installer, or another companion. `carrier` is a neutral display label; it does not identify an actual network or request number sharing. The original screen currently starts with Fi-themed content regardless of this label.

Codes must match the original app's supported `LPA:1$server$matching-id` grammar (the `LPA:` prefix is optional). The matching ID is restricted to uppercase letters, digits, and hyphens by that version's parser; lowercase IDs are rejected rather than changed. The helper additionally rejects malformed server URLs, paths, and control characters. Profiles marked as requiring a separate confirmation code are blocked because this direct destination does not expose an entry field. A provider may require a confirmation code even without that marker; such requests can still fail in the original app. Watch reboot handling may differ from the full supported QR setup flow.

## Privacy and scope

- Only camera access; no internet, Bluetooth, phone state, or eSIM management permissions.
- Camera scanning is embedded and offline (JourneyApps + ZXing).
- No activation-code files, preferences, analytics, or logging in the helper.
- Screenshots of the helper are disabled. Codes are excluded from saved instance state and backup, and cleared after handoff or when leaving the helper (except while its scanner is open).
- The original app receives the activation code only after your explicit confirmation. Its own logging and data handling are outside the helper's control.
- Camera access is optional; manual input works without it.

No eSIM is written directly by the helper. A successful handoff delegates installation to the original signed app and the watch's provisioning service. Phone eSIM storage is never accessed. The helper cannot guarantee that a carrier's transfer/replacement code will leave the phone's service unaffected.

## Build

Use JDK 17 and an Android SDK with platform 35 and build tools 35.0.0. Set `ANDROID_HOME` to your SDK, or set `sdk.dir` in untracked `local.properties`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Pinned tool versions: Gradle 8.11.1, Android Gradle Plugin 8.9.3, JourneyApps ZXing embedded 4.3.0, ZXing core 3.5.3, AndroidX Core 1.13.1, AppCompat 1.7.0, JUnit 4.13.2, Robolectric 4.14.1. The debug signing key is created under `.local/` and is excluded from the source archive. Rebuilding with a new key requires uninstalling the helper's previous build, not Pixel Watch. This project is an Android Studio importable Gradle project.

## Verification

See `TESTING.md` for actual results and the device tests still needed. Unit tests cover QR decode, parser boundaries, exact intent encoding, explicit companion targeting, missing/disabled target handling, review/confirmation, changed input, and credential clearing on recreation/backgrounding. Android-dependent tests run with Robolectric at API 30 and API 35.

## Evidence

The initial companion analysis is preserved in [docs/companion-analysis.md](docs/companion-analysis.md). The central trace is `ixu` → `ipd.h()`/`kvy.j()` → `ipb` → `kzm`/`kzl` → `xfn.L()` → `xea.c()`/`xea.t()`. The external destination registry includes `ipd.h()` in `igs`, case 11. The vendor APK, decompiled sources, firmware archives, and generated packages are not part of this source repository. See `TESTING.md` for subsequent manual testing.

## Dependency licenses

JourneyApps ZXing Android Embedded and ZXing Core are Apache-2.0 licensed. See `THIRD_PARTY_NOTICES.md` for project links and attribution. The helper code is provided under the Apache-2.0 license in `LICENSE`.
