# Watch-only QR eSIM feasibility analysis

## Conclusion

A carrier-independent QR option is technically plausible in the supplied Google Pixel Watch companion APK. The APK already contains the scanner, manual input, confirmation-code handling, metadata preflight, and a download RPC addressed to the linked watch. The main visible restriction is companion-app policy that derives provisioning choices from the phone's active carrier and its configured setup mechanisms.

This is static feasibility evidence, not a demonstrated patch or successful installation. Offering QR setup for every connected, eSIM-capable watch is achievable in principle; making every provider's profile install and work is not guaranteed. There is no watch-side APK, device, or runtime capture in the folder.

## Target and method

- Package: com.google.android.apps.wear.companion
- Version: 5.0.0.958206140; versionCode 331702839
- minSdk 30, targetSdk 35
- APK SHA-256: 265ba30c0b79c51d89e115523ef2158839adb581896e2e24591eb6b05fa9cbf2
- The folder originally contained only this APK, not a buildable source project.
- REA inspect_android_package was attempted but unavailable because the server has no configured jadx-headless-mcp JAR. Analysis used temporary JADX CLI 1.5.3 instead. The app was never executed.
- Decompiled output contains 68 reported errors and some coroutine methods have control-flow/type-recovery warnings. The relevant request construction, endpoint constants, and basic gates are readable. Generated Java is an analysis representation, not build-ready source.

## Existing architecture relevant to eSIM

The companion model receives watch identity and installed-profile data, reads the phone's active subscriptions, matches carrier configurations, and offers QR, Fi, ODSA (carrier enrollment), or default SM-DP+/SM-DS flows. Carrier enrollment is not simply copying the phone's existing eSIM: it obtains/provisions a separate watch profile, often with shared-number service.

Evidence locations (relative to analysis/decompiled/sources/defpackage):

1. xgo.c(): reads SubscriptionManager.getActiveSubscriptionInfoList(), MCC/MNC, carrier ID, slot and display name. xgo.g()/h(): carrier matching. This is phone subscription inspection.
2. xfn.M(): getEligiblePhoneSubscriptionList; filters missing carrier config, missing provisioning methods, unsupported companion service, already matched watch profiles, and device eligibility.
3. xcy.H(): QR availability is true only when the selected carrier config's mechanism list contains xcp.a (QR_CODE). Missing config returns false.
4. xfn.s(): waits for valid watch information and profile data; QR is added only when no relevant existing carrier-matched profile is found and xcy.H() is true. xfn.K(): rejects an empty method list.
5. nyw.b(), case 5: setup-options screen checks xfc.l(true) and exits if provisioning is unavailable. nzr.c() constructs QR/on-device options; nzh.invoke(), case 9 starts xfp.a.
6. xfd.b(): crucially, QR is already exempt from the supported-method-list rejection. It still requires valid watch eSIM information. It constructs xgg, xmf (scanner), xme (manual entry), xfx (confirmation code), and xgi.b() (metadata/confirmation/download sequence), even if the immutable carrier config is null.
7. kzy.b(): checks valid paired watch and connection before starting provisioning; current navigation labels it SHARED_NUMBER. This does not by itself create number sharing: that depends on carrier provisioning.

## QR to watch data flow

Phone camera/photo QR or manual input
  -> xmf.i()/o() or xme.f()
  -> xgo.q()/i() validates LPA activation-code format and confirmation-code marker
  -> xgg.a holds activation code; xga holds confirmation code/download state
  -> xfv.b() asks xdl.e(watchNodeId, activationCode, confirmationCode) for metadata
  -> xea.e()/u() sends /esim/rpc/profile_metadata_request to that peer
  -> successful QR metadata returns xdh(carrierName) and is accepted by xfv
  -> user confirms download
  -> xfy.b(), xgg branch calls xdl.c(watchNodeId, activationCode, confirmationCode, carrierRebootFlag, true)
  -> xea.c() creates pjf protobuf; xea.t() sends it to watchNodeId via piv.n
  -> /esim/rpc/profile_download_request
  -> acknowledgement and /esim/rpc/profile_download_response
  -> watch result supplies downloaded ICCID or failure.

The QR-success metadata path returns xdh directly. xfv also contains an xdc carrier-name allowlist branch, but xea.u(), used by QR metadata, does not produce xdc. Do not indiscriminately remove metadata validation or confuse that separate branch with the QR flow.

xgo recognizes optional LPA: prefix and a 1$server$matching-id activation string (with optional suffixes), and limits the extracted activation string to 255 characters. Barcode recognition supports camera and Bitmap input through Barhopper or ZXing. The existing UI also includes manual entry and select-from-photos strings.

## Phone eSIM isolation

In the recovered sources, a bounded whole-tree search found no references to EuiccManager or downloadSubscription(), switchToSubscription(), or deleteSubscription(). The manifest requests READ_PHONE_STATE and CAMERA but does not request WRITE_EMBEDDED_SUBSCRIPTIONS. The traced QR mutations use the watch peer RPC, not a local phone eUICC manager.

An independent QR flow should reuse that watch RPC and never launch the phone's eSIM activation activity or local LPA. Android's standard EuiccManager targets an eUICC on the device where it runs; createForCardId selects a local card, not a Bluetooth watch. EXTRA_USE_QR_SCANNER does not turn a phone's activation UI into remote-watch provisioning.

Local phone profiles can be kept untouched by design. A stronger promise that the phone's service can never be affected requires a separate watch subscription/QR: using a replacement or transfer code for the phone's existing line could cause the carrier server to deactivate that line even though the companion never edits phone eSIM storage. A QR is an activation credential, not a reusable copy of an installed eSIM.

## Proposed implementation scope

- Add an explicit Add watch eSIM via QR entry on mobile-network settings/setup independent of eligible phone subscription rows. It should be reachable with unsupported carriers and with no phone SIM.
- Derive QR availability from the selected connected watch's eSIM capability and synchronized state, rather than xcy.H(), phone carrier eligibility, or whether a same-carrier watch profile is already installed. Audit both setup-screen and model availability guards so they agree.
- Route directly into xfp.a using the current watch's model and node. xfd already supports QR with absent carrier config. Do not globally fake a supported phone carrier or enable ODSA/Fi.
- Preserve QR validation, confirmation code, remote metadata preflight, user confirmation, transport error handling, and watch capacity/capability checks. Use carrier-neutral labels until watch metadata identifies the provider.
- Explicitly explain any disabling of an existing WATCH profile. Preserve existing watch profiles unless the user chooses a supported switch; never delete one merely to expose QR.
- Audit post-install carrier mismatch prompts and profile management. Existing xfn.J() matches watch profiles to phone subscriptions, and strings invite the user to get both devices on the same network. A standalone watch profile must remain usable without forcing another download or carrier enrollment.
- Keep all profile installation/enable/delete actions pinned to the same selected watch node. If it disconnects or selection changes, stop rather than fall back to a phone installer or another watch.
- Avoid logging raw activation/confirmation codes; some existing debug messages include them.

## Remaining limits and verification

The watch must have an eUICC, compatible firmware, and a working privileged watch-side provisioning service. Its firmware may still impose carrier policies, reject profiles, or require a reboot. SM-DP+ may bind a profile to an EID or device type or reject an already-used activation code. Successful download does not establish LTE registration, VoLTE, messaging, roaming, or shared-number functionality. Shared-number service requires carrier support.

Because only a signed vendor APK is supplied, implementation entails a binary patch/rebuild/re-sign rather than normal source changes. A modified signing certificate cannot update the installed Google-signed app directly and may affect identity-dependent pairing or Google services. Such checks have not been tested here. No patched APK was produced and no device profiles were changed.

A meaningful runtime test should cover an unsupported phone carrier, no SIM, two phone SIMs, and existing watch profiles; trace the watch RPC and verify the requested watch EID/ICCID. Compare phone installed profile ICCIDs, enabled state, default subscriptions, and service before/after success and failure. Test malformed/consumed QR, confirmation-code requirement, disconnected watch, and watch selection change. Use a distinct disposable watch plan so carrier-side transfer semantics cannot affect the phone line.

## Primary platform references

- Android EuiccManager: https://developer.android.com/reference/android/telephony/euicc/EuiccManager
- Android eSIM architecture and permissions: https://source.android.com/docs/core/connect/esim-overview
- Google Pixel Watch cellular setup and supported service: https://support.google.com/googlepixelwatch/answer/13541778?hl=en

## Follow-up: installation, restoring stock app, and unmodified entry point

A later trace found a concrete alternative to patching the APK: the exported RootActivity accepts https://madeby.google.com/wear-companion/navigation deep links. izb extracts dest and resolves it through the destination registry. igs.b(), case 11 includes ipd.h(); that constructs ixu(kvy.j()). ixu registers external destination esim_download. kvy.j() constructs the internal download_esim?activationCode={activationCode}&carrier={carrier} screen. iyh/ksr.cb forwards query parameters into a Bundle. ula sends the destination to main navigation; kwj resolves that registered destination and builds the internal route. ipb case 12 renders ksr.T, which creates kzm. kzm reads activationCode and uses the current watch model. Its setup action invokes kzl -> xfc.y(code) -> xfn.L -> xdl.c(watchNodeId, code, null, false, true) -> watch download RPC. This path does not invoke phone-carrier eligibility in the visible download chain. Runtime pairing, destination handling and watch-side acceptance remain untested.

Candidate URI (encode activationCode and carrier as query parameter values):
https://madeby.google.com/wear-companion/navigation?dest=esim_download&activationCode=<URL-encoded-LPA-code>&carrier=<URL-encoded-label>

A helper APK could scan/validate the QR and launch an explicit ACTION_VIEW intent addressed to com.google.android.apps.wear.companion.core.application.RootActivity in com.google.android.apps.wear.companion, with that URI. The original signed app would perform the privileged communication. This is a statically traced entry point, not a verified installation technique. The screen starts with Fi-themed UI (kzr), despite accepting a supplied activation code. The direct path passes no separate confirmation code, unlike the full QR flow, so confirmation-code-required profiles may fail. The companion carrierRebootFlag is false on this path. Do not assume its UX or post-install behavior matches full QR provisioning.

Signature caveat stronger than the original preliminary conclusion: xea uses vvp implemented by vvu (MessageClientSender), backed by Google Play services wearable APIs. Standard Wear Data Layer communication requires matching package name and signing certificate across apps on the two devices. Google companion/home integration may use privileged vendor routing; a newly signed replacement or helper cannot assume it inherits that trust. A pure UI patch might install after uninstalling the stock phone app yet fail to communicate with the stock watch service. An external helper that delegates to the original signed app avoids replacing its transport identity.

A re-signed APK with the original package name cannot update the Google-signed app under normal Android rules. Uninstall/reinstall removes local companion state and can require re-pairing/recovery. Changing the package allows side-by-side installation in principle but does not preserve Google's transport identity or integration. Returning to stock similarly requires removing the re-signed package first. Uninstalling the phone app does not itself establish a watch eSIM erase operation; recovery flows that reset/erase watch profiles need separate assessment.

Stock app reconciliation: xfn.J() compares the watch's active profile to phone subscriptions, with carrier-ID/MCC-MNC checks in xgo and additional ICCID handling for certain providers. It detects a mismatch, not blindly assumes equality. The UI contains get-watch-and-phone-on-the-same-network and switch/download suggestions. xfn.w()/m() restrict carrier subscription-management features based on available carrier config and matching phone subscriptions. No automatic erase/disable merely due to carrier mismatch was established by this bounded trace; this does not prove absence across all firmware and recovery flows. Basic profile operations and carrier account/number-sharing operations are distinct. Shared-number service does not arise from installing an arbitrary standalone QR. A watch may retain independent connectivity while the companion displays a mismatch or lacks carrier-management options.
