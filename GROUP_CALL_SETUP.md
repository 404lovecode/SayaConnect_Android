# SayaConnect v2 — Group voice + video call upgrade

**What this source includes:** the existing private 1:1 calling and chat, plus **3–4 person** group video or voice calls. Group calls are invite-only and members must be accepted friends. The host can end the room for everyone; guests can decline or leave. Peer-to-peer media is WebRTC encrypted and Firebase Firestore holds the *signaling* metadata, not video recordings.

## Start a call

1. Create accounts and accept friendship requests with at least **two** friends.
2. Open **Friends → Group call**.
3. Choose **Video** or **Voice**, select **2–3 friends**, tap **Start call**.
4. Invited friends receive a pop-up while the app is open (and an FCM push if configured). They can **Answer** or **Decline**.
5. Each joined person appears as a tile. Use **Mic**, **Camera** (video), **Flip**, and **React**. Tap **End group call** as host, or **Leave call** as a guest.
6. Under **Settings**, turn on low-data mode *before starting*; group video uses ~160×120, 8 fps and a target 100 kb/s **per remote peer**, plus audio and protocol overhead. It is not a fixed bandwidth guarantee.

## First setup / Firebase changes

* Follow README_EN.md (or README_MY.md) for Android Studio and Firebase configuration. This remains an **Android Studio source project**, **not a compiled APK**.
* **REDEPLOY the updated Firestore rules AND functions** after upgrading from v1. Use PowerShell inside `backend`:

  ```powershell
  firebase deploy --only firestore:rules,functions
  ```

* Firestore stores `groupCalls/{groupId}` with an immutable invitation list, member states, one `pairs/{id}` document per active peer pair, ICE candidate subcollections, and reactions. Do not deploy the app without the included updated security rules.
* FCM sends a `group_call` notification to up to three invitees through the new `notifyIncomingGroupCall` Cloud Function; a background push brings users back to the app, **not an automatic background call acceptance**.
* Camera / microphone runtime permissions are required. Voice-only calls request microphone access only.
* A **reachable TURN relay** is necessary for many mobile carrier networks and for some international connections. The existing `TURN_URL`, `TURN_USER`, `TURN_PASSWORD` build properties are a *testing mechanism*. Never publish long-lived TURN passwords embedded in your APK.

## GitHub build (optional)

The archive now includes `.github/workflows/android-debug.yml`. GitHub **stores and builds** the Android app, but does **not host real-time calling**. Firebase and your STUN/TURN endpoints must be live for the installed APK to communicate.

1. Upload the **contents** of the `SayaConnect_Android_Studio` folder into the root of your GitHub repository.
2. In GitHub → repository → Settings → Secrets and variables → Actions, add a secret called **`GOOGLE_SERVICES_JSON_B64`** containing Base64-encoded `app/google-services.json`. Example Windows PowerShell conversion (do not paste this private value into a public README):

   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes('app\\google-services.json'))
   ```

3. Click **Actions → Android debug APK (SayaConnect) → Run workflow**. After a successful build, download the `SayaConnect-group-call-debug` artifact and extract `app-debug.apk`.
4. APK building will work only after the correct Firebase configuration, SDK dependencies and Gradle wrapper are available. The included wrapper is a custom bootstrap shim; for a standard wrapper, regenerate with `gradle wrapper --gradle-version 8.10.2` on your computer.

## Performance, privacy & known limitations

* Uses an encrypted **peer-to-peer mesh**: every device uploads separate media to every other participant. At 4 people that is 3 outgoing video connections per device. **A larger group requires a proper SFU** such as LiveKit, Janus, or mediasoup and server infrastructure; do not simply increase the four-person limit.
* Voice-only is generally more reliable than video when speeds or latency are poor. If the phone has **no usable internet**, calls cannot work. Firestore, Firebase Auth and TURN must also be reachable; **no VPN-free access guarantee** in Myanmar or elsewhere.
* Group call functions work while the app is in the foreground. This MVP does **not** implement persistent call services, automatic reconnect after the app is killed, dynamic invite additions, recordings, host transfer, active-speaker detection, message-to-call group chats, or rejoining after leaving.
* There is no integrated SFU, no custom TURN deployment and no ready-to-install APK. **Real 3–4 device tests, Firestore rules emulator checks, and a full Gradle build remain required**. No Android SDK, Gradle dependencies, Firebase credentials or physical devices were available here.
* Firebase events and signaling are **not end-to-end encrypted**, although WebRTC media uses DTLS-SRTP. Consider app integrity protection, abuse prevention, Firebase quotas and server-issued ephemeral TURN credentials before public release.
