# SayaConnect v3: Email-optional login (English)

This source upgrade adds 3 entry paths. It is **not yet a tested APK**.

## Entry modes

1. **Phone number + six-digit SMS code:** sign up/sign in on a different device without an email. Enter an international/E.164-style phone number (`+959...` for Myanmar, `+66...` Thailand, `+90...` Türkiye, `+84...` Vietnam); for Myanmar don't include the domestic leading 0 after `+95`. Some numbers may not be reachable for verification.
2. **Guest (Firebase anonymous auth):** use SayaConnect immediately without email or SMS, but the guest account is device/session-bound. **Sign out, uninstalling or clearing app storage may make an unlinked guest account unrecoverable.** Users should link a phone before changing devices. Guests can create a profile and access the same starter features, subject to existing Firestore and app rules.
3. **Email/password:** backward-compatible existing accounts.

**Username is still a unique public profile identifier for finding friends, NOT a password-based login credential.** An actual username/password login and robust account recovery would require an additional secure backend and security reviews; do not simulate it with fake Firebase email addresses.

## Firebase Console configuration required (new)

1. Firebase Console → **Authentication → Sign-in method**: enable **Phone**, **Anonymous** and **Email/Password**. Do not disable Email/Password if existing members already use it.
2. Firebase Console → **Authentication → Settings → SMS region policy**: allow countries whose phone numbers your service supports, e.g. Myanmar (MM), Thailand (TH), Türkiye (TR), Vietnam (VN). New projects may block all countries by default; review SMS abuse risks and only enable needed countries.
3. Use the Firebase **Blaze (pay-as-you-go)** project for sending verification SMS; phone SMS verification is chargeable and has quotas. Set budget alerts, monitor use and enable Firebase App Check for production.
4. Firebase Console → Project Settings → Your apps → Android application `com.sayaconnect.app`: register **SHA-1 and SHA-256** fingerprints for every Android signing key used (local debug, GitHub Actions debug, release). Play Integrity and reCAPTCHA fallback require correct verification setup. On APKs installed outside the Play Store, ensure reCAPTCHA can open properly. Configure the API key / Firebase Auth domain if prompted.
5. Existing `app/google-services.json` is still required and must correspond to the same Firebase project used by existing users. Keep existing Firestore rules; their `signed()` check already accepts Firebase-authenticated anonymous accounts. Only deploy the existing backend rules if they are new or otherwise need updating; this upgrade does not require new collections/functions.
6. Build via Android Studio or the provided GitHub Actions debug workflow. If using GitHub, add the `GOOGLE_SERVICES_JSON_B64` Actions repository secret. **For consistent phone-auth behavior across GitHub builds, also add `ANDROID_DEBUG_KEYSTORE_B64`, a base64-encoded copy of a stable debug keystore (`~/.android/debug.keystore` on Windows typically `%USERPROFILE%\.android\debug.keystore`), and register that exact key's SHA-1/SHA-256 fingerprints with Firebase. Never commit your keystore or its base64 value.** SMS checks require running on a real device or Firebase's fictitious test phone numbers configured for test use. **Do not hardcode SMS verification codes** or disable app verification in production.

## Guest recovery

A signed-in guest opens **Settings → Account security → Link phone number to this account**, verifies the SMS, and can later sign in with the same phone number. Linking retains the Firebase UID, existing profile, friends, and messages. Linking an already-registered phone number fails intentionally to avoid silently overwriting another account. Do not sign out before linking successfully.

**Known limitation:** the linking operation can be affected by a documented Firebase issue for some projects; test carefully before a public release: https://firebase.google.com/docs/auth/android/account-linking . If linking fails, keep the guest signed in and contact support; don't discard the guest's data. Account recovery from a guest device already uninstalled/signed out is not implemented.

## Test checklist

- New guest → choose username → add friend → chat → Settings → Link phone → verify → sign out → phone login again → verify UID/profile restored.
- New phone sign-in with configured fictitious test number → username → sign out → sign in with same number → existing profile appears.
- Existing email login continues to work. Linking a phone to an email account preserves the same profile.
- Verify bad OTP, invalid international format, SMS blocked/undelivered, quota exceeded, reCAPTCHA fallback, and phone already used on another account.
- Verify a guest sees a sign-out warning; test keyboard/display with Burmese and English.
- Test carrier networks in Myanmar; SMS and Firebase connectivity can fail due to blocking, no coverage, unsupported devices or network conditions. VPN-free connection is **not guaranteed**.

## Privacy / launch requirements

Phone authentication uses the number for verification, and Firebase/Google may process the number for abuse prevention. Disclose this in a privacy policy and obtain consent as applicable. Firebase phone-only login is not equivalent to a hardware-backed authenticator; recycled SIMs/number takeovers are possible. Guest accounts create abuse-management risks: add App Check, message/request/call server-side rate limits, reporting, blocking, deletion, and moderation before wide distribution. Current Firestore messages are not end-to-end encrypted.

Official docs: https://firebase.google.com/docs/auth/android/phone-auth , https://firebase.google.com/docs/auth/android/anonymous-auth , https://firebase.google.com/docs/auth/limits
