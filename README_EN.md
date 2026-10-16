# SayaConnect — Android Studio Source (v3.0, Email-optional Login + Group Calls)

**NEW in v3:** Phone + SMS one-time code, email/password, and guest (anonymous) entry, with phone linking for guest account recovery. See `LOGIN_OPTIONS_SETUP.md` and `LOGIN_OPTIONS_SETUP_MY.md` for exact Firebase config, Myanmar carrier notes, billing, privacy and testing. Existing accounts continue to use the same Firebase project.

**NEW in v2:** Group voice/video calls for 3–4 total users; from **Friends → Group call**, select 2–3 friends. See `GROUP_CALL_SETUP.md` (and `GROUP_CALL_SETUP_MY.md`) for steps, Firebase redeploy, limitations and GitHub Actions APK build.

**A real Android/Kotlin starter implementation, not a compiled APK.** It contains a Firebase-backed social/chat client, a WebRTC calling engine, server translation endpoint, FCM call alerts, and an interactive **design-only** preview in `preview/index.html`. The Android code **has not been compiled or device-tested in this packaging environment** (there is no Android SDK / internet access here). Please test with two real Android phones before distributing it.

## Windows installation: exact order

1. Install **Android Studio** and its **Android SDK 35**, **Platform Tools**, and **JDK 17**. Unzip this archive to a directory such as `C:\SayaConnect` and choose **Open** in Android Studio. Select the folder that contains `settings.gradle.kts` (not `backend`). Let the IDE sync/download Gradle and Maven dependencies once.
2. Open [Firebase Console](https://console.firebase.google.com/) and **create a new Firebase project**. Add an **Android app** with **package name `com.sayaconnect.app`**. Download the private `google-services.json` from Project Settings → Your apps. Copy it to **`SayaConnect/app/google-services.json`**. **Never publish this config as a publicly editable app credential; keep backend secrets off the client.**
3. In Firebase, enable **Authentication → Sign-in method → Email/Password, Phone, Anonymous** and set the SMS region policy + SHA signing fingerprints (see `LOGIN_OPTIONS_SETUP.md`); create **Cloud Firestore**; create **Cloud Storage** (a Storage bucket and billing may be necessary for new projects); and enable **Cloud Messaging**.
4. Install Node.js 22 and Firebase CLI on Windows (`npm install -g firebase-tools`). Open PowerShell in `SayaConnect\backend`, run `firebase login`, `firebase use --add` (choose your Firebase project), then `npm --prefix functions install` and `firebase deploy --only firestore:rules,storage,functions`. The Firebase CLI may prompt you to enable required APIs / paid Blaze plan for Cloud Functions.
5. For **Burmese and other auto-translation**, enable **Cloud Translation API** and billing in the **same Google Cloud/Firebase project**, and grant the runtime service account permission to call Cloud Translation. Server code is in `backend/functions/index.js`. Translation is disabled by default; in app settings toggle it on. Server quota is **200 translated messages per user per UTC day**. Your Google Cloud account may be charged. The app shows the original text if translation fails.
6. For more reliable calls across different mobile operators, set up **your own TURN relay** (e.g., coturn; secure `turns:` URL with TLS). In the root `gradle.properties` add:

   ```properties
   TURN_URL=turns:YOUR_TURN_DOMAIN:5349
   TURN_USER=YOUR_TEST_ACCOUNT
   TURN_PASSWORD=YOUR_TEST_PASSWORD
   ```

   **Security:** These static values are embedded in the APK and can be extracted. Use **short-lived, server-issued TURN credentials** before a public production launch. The included STUN-only fallback might work on some networks but may fail completely with carrier-grade NAT or restricted networks.
7. Click **Sync Project with Gradle Files**. Connect an Android phone with USB debugging or use an emulator (Android 8.0/API 26+). Click **Run ▶**. To generate an APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**; the debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk` *after successful build*.
8. Install it on two Android devices; sign up with two distinct test accounts (phone/email/guest), create unique usernames, search for a username in **Explore**, send a friend request, accept it in **Friends**, open a chat, and try a video call. Grant camera/microphone permission and notification permission when prompted.

### Gradle bootstrap note

`gradle/wrapper/gradle-wrapper.jar` is a **small, explicitly included, open-source-style bootstrap shim** whose Java source is shipped in `gradle/bootstrap-src/`. We could not download the official wrapper binary into the packaging environment. It downloads **only the official Gradle 8.10.2 distribution** and validates its **official SHA-256** before running it. For a normal official wrapper, run `gradle wrapper --gradle-version 8.10.2` in the project once you have Gradle installed; this will replace the shim. If your IDE refuses to trust the bootstrap, set a local Gradle 8.10.2 installation under Gradle Settings and regenerate the wrapper.

## Features included

- Phone/SMS sign-in, Firebase anonymous (guest) sign-in, and email/password sign-in, account-linking option in Settings, personal profiles, unique `@username` reservation, display name, profile avatar, and **100-word bio validation in the app**.
- Friend finding by exact username, friend request/acceptance, real-time Firestore one-on-one messages, messages in any Unicode language, and in-chat emoji picker.
- **Optional automatic translation** using a Firebase callable endpoint backed by Cloud Translation; language targets include Burmese, English, Thai, Chinese, Japanese, Korean, Hindi, Bengali, Indonesian, Vietnamese, Spanish, French, German, Arabic, Portuguese, Russian and Turkish. The original message remains visible.
- Group **WebRTC video/voice calls** with friend invitations, 2×2 tiles, mute/camera controls, emoji reactions and low-bandwidth mesh capped at **four total participants**. This requires the updated Firestore rules and Functions deployment.
- One-to-one **WebRTC video/audio** over mobile data or Wi-Fi, 2 STUN servers, configurable TURN, mute/camera toggle, camera switch, emoji reactions, and **low-data mode** (approx. 320×240 video, 10 fps, max 150 kbps encoder target; real network overhead and adaptation vary).
- Live foreground incoming call dialog, FCM push notifications for calls when supported, profile music upload to Storage or HTTPS audio URL + playback, image upload, light/dark/system color scheme, custom device image or gradient chat wallpapers, English/Burmese core UI labels.
- Secure-by-default **starter Firestore and Storage rules**. These are a baseline, *not a security audit*. Enable Firebase App Check, rate limits for friend requests/calls, abuse reporting, blocking, moderation, consent and account deletion before public release.

## Important real-world limitations

- **No VPN bypass guarantee.** This app needs internet. Some networks in Myanmar or elsewhere may block Firebase, Google services, your TURN server or international traffic. TURN can solve NAT traversal but is **not a censorship bypass**; connectivity needs testing from actual Myanmar operators, and local legal/regulatory requirements may apply.
- **Poor internet is not zero internet.** Low data mode helps but cannot promise stable video over extreme packet loss. The app is a foreground calling MVP, not a carrier-grade calling product. Persistent calls while the Android app is fully backgrounded need a foreground service and additional work.
- Firebase chat and signaling are protected by authentication and server rules and transport TLS, but **Firestore text messages are not end-to-end encrypted**. WebRTC media uses DTLS-SRTP. Do not advertise the whole application as E2EE.
- Burmese translation is **not available in ML Kit on-device translation**; the included solution uses Cloud Translation, which needs deployment, connectivity and billing.
- Music uploads need an enabled Firebase Storage bucket, and user-generated music should respect copyright. Audio uploads limited to 15 MB; images 5 MB.
- Username is fixed after account creation in this MVP; you can change display name and bio. Chat message history stores up to the last 150 in the current screen. No search indexing/list of all conversations is supplied.
- Tests, analytics, crash monitoring, OAuth login, username/password login, read receipts, end-to-end encrypted text, spam prevention, moderation and app-store publishing are not yet implemented.
- **No ready-to-install APK is inside this ZIP.** Android Studio and your own Firebase project are required.

## Folder map

```text
app/src/main/java/com/sayaconnect/app/
  MainActivity.kt                  Login-state, routing, permissions, incoming call UI
  data/ConnectRepository.kt       Firebase Auth/Firestore/Storage/Functions code
  data/Models.kt                  Data models
  data/SettingsStore.kt           Local preferences and localization strings
  rtc/RtcSession.kt               WebRTC capture, signaling, SDP/ICE, bitrate, TURN
  ui/Screens.kt                   Auth methods, friends, messages, search, profile, settings
  ui/AuthExtras.kt                Phone OTP verification and account linking
  ui/CallScreen.kt                In-call UI, reactions, camera and mic controls
  ui/Theme.kt                     Dark/light palette, wallpaper backgrounds
  notifications/CallPushService.kt Android FCM call notifications
backend/firestore.rules           Restricted Firestore access model
backend/storage.rules             User-owned Storage writes
backend/functions/index.js        Server translation and FCM call notifications
preview/index.html                Mock design demo (not connected to a backend)
```

## Suggested first tests

- User A account, User B account; both online, try sign in/out and duplicate usernames.
- Ask B for friendship, accept, send Burmese+English texts, enable translation and verify **original + translated** display.
- Call on same Wi-Fi, then two carriers / international Wi-Fi vs Myanmar mobile network; test audio, camera flip and reactions.
- Turn data off during a call and on again; verify reconnect state, hangup and cleanup.
- Check background call notification on Android 13+ with notification permission and Cloud Functions deployed.
- Upload MP3 and avatar, view the friend's music via exact username search. Switch wallpapers and app languages.

Please do not publish to Google Play without building, testing and security-reviewing the app. SMS costs money (Firebase Blaze required). Guest accounts are not recoverable unless linked to another authentication method before sign-out/data deletion.
