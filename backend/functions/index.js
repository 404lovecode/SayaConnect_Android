'use strict';
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {onDocumentCreated} = require('firebase-functions/v2/firestore');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getMessaging} = require('firebase-admin/messaging');
const {Translate} = require('@google-cloud/translate').v2;
const {initializeApp} = require('firebase-admin/app');
initializeApp();
const db = getFirestore();
const translator = new Translate();
const REGION = 'asia-southeast1';
const LANGS = new Set(['my','en','th','zh','ja','ko','hi','bn','id','vi','es','fr','de','ar','pt','ru','tr']);

// Authenticated, metered server translation. Never ship Cloud service credentials in the APK.
exports.translateText = onCall({region: REGION, maxInstances: 5, timeoutSeconds: 45}, async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Please sign in.');
  const text = request.data?.text;
  const target = request.data?.target;
  if (typeof text !== 'string' || text.length < 1 || text.length > 4000 ||
      typeof target !== 'string' || !LANGS.has(target)) {
    throw new HttpsError('invalid-argument', 'Invalid text or target language.');
  }
  const day = new Date().toISOString().slice(0, 10);
  const ref = db.collection('translationUsage').doc(`${request.auth.uid}_${day}`);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const count = snap.exists ? snap.data().count : 0;
    if (count >= 200) throw new HttpsError('resource-exhausted', 'Daily translation limit reached.');
    tx.set(ref, {count: count + 1, updatedAt: FieldValue.serverTimestamp()});
  });
  try {
    const [translated] = await translator.translate(text, target);
    return {translated};
  } catch (err) {
    console.error('Translation failed:', err?.message);
    throw new HttpsError('unavailable', 'Translation service is not configured or reachable.');
  }
});

// Incoming call notification when the recipient is not looking at the app.
exports.notifyIncomingCall = onDocumentCreated({
  document: 'calls/{callId}', region: REGION, maxInstances: 10
}, async (event) => {
  const call = event.data?.data();
  if (!call || !call.to || !call.from) return;
  const tokenSnap = await db.collection('pushTokens').doc(call.to).get();
  const token = tokenSnap.data()?.token;
  if (!token) return;
  try {
    await getMessaging().send({token, data: {
      kind: 'call', callId: event.params.callId,
      fromName: String(call.fromName || 'A friend').slice(0, 70)
    }, android: {priority: 'high'}});
  } catch (err) {
    console.warn('Call FCM failed:', err?.code);
  }
});

// Each invited group member receives a push (up to three recipients per room).
// The client still checks its Firestore invitation state before accepting.
exports.notifyIncomingGroupCall = onDocumentCreated({
  document: 'groupCalls/{groupId}', region: REGION, maxInstances: 10
}, async (event) => {
  const call = event.data?.data();
  if (!call || call.status !== 'active' || !Array.isArray(call.members)) return;
  const recipients = call.members.filter(uid => uid !== call.host).slice(0, 3);
  await Promise.all(recipients.map(async uid => {
    const tokenSnap = await db.collection('pushTokens').doc(uid).get();
    const token = tokenSnap.data()?.token;
    if (!token) return;
    try {
      await getMessaging().send({token, data: {
        kind: 'group_call', callId: event.params.groupId,
        fromName: String(call.hostName || 'A friend').slice(0, 70)
      }, android: {priority: 'high'}});
    } catch (err) {
      console.warn('Group call FCM failed:', err?.code);
    }
  }));
});
