package com.sayaconnect.app.data

import android.net.Uri
import android.app.Activity
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class ConnectRepository {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val functions = FirebaseFunctions.getInstance("asia-southeast1")
    val uid get() = auth.currentUser?.uid.orEmpty()

    suspend fun signUp(email: String, password: String) { auth.createUserWithEmailAndPassword(email.trim(), password).await() }
    suspend fun login(email: String, password: String) { auth.signInWithEmailAndPassword(email.trim(), password).await() }
    /** No email or SIM is required, but anonymous accounts are not portable until linked. */
    suspend fun loginAsGuest() { auth.signInAnonymously().await() }

    fun sendPhoneCode(
        activity: Activity,
        number: String,
        onCodeSent: (String) -> Unit,
        onAutoVerified: (PhoneAuthCredential) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        require(Regex("^\\+[1-9][0-9]{6,14}$").matches(number)) {
            "Enter the full number with country code, e.g. +959xxxxxxxxx"
        }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) = onAutoVerified(credential)
            override fun onVerificationFailed(e: FirebaseException) = onFailure(e)
            override fun onCodeSent(
                verificationId: String, token: PhoneAuthProvider.ForceResendingToken
            ) = onCodeSent(verificationId)
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity) // Required for reCAPTCHA fallback.
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyPhoneCode(verificationId: String, smsCode: String, linkToCurrent: Boolean) {
        require(smsCode.matches(Regex("^[0-9]{6}$"))) { "Enter the 6-digit SMS code" }
        usePhoneCredential(PhoneAuthProvider.getCredential(verificationId, smsCode), linkToCurrent)
    }

    suspend fun usePhoneCredential(credential: PhoneAuthCredential, linkToCurrent: Boolean) {
        if (linkToCurrent) {
            val current = auth.currentUser ?: error("Please sign in first")
            require(current.phoneNumber == null) { "A phone number is already linked" }
            // Linking rather than signing in prevents the loss of guest profile and friendships.
            // If already used by a different account, fail instead of silently replacing UID.
            current.linkWithCredential(credential).await()
        } else {
            auth.signInWithCredential(credential).await()
        }
    }

    fun logout() { auth.signOut() }

    fun observeMe(userId: String, cb: (Person?) -> Unit): ListenerRegistration =
        db.collection("users").document(userId).addSnapshotListener { snapshot, _ -> cb(snapshot?.person()) }

    suspend fun createProfile(username: String, displayName: String, bio: String) {
        val handle = username.trim().lowercase()
        require(Regex("^[a-z0-9_]{3,20}$").matches(handle)) { "Username: 3–20 lowercase letters, numbers or _" }
        require(displayName.trim().length in 2..50) { "Name must be 2–50 characters" }
        require(bio.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size <= 100) { "Bio must be 100 words or fewer" }
        val user = auth.currentUser ?: error("Sign in first")
        db.runTransaction { tx ->
            val handleRef = db.collection("handles").document(handle)
            val userRef = db.collection("users").document(user.uid)
            check(!tx.get(handleRef).exists()) { "Username is already taken" }
            check(!tx.get(userRef).exists()) { "Profile already exists" }
            tx.set(handleRef, mapOf("uid" to user.uid))
            tx.set(userRef, mapOf(
                "username" to handle, "displayName" to displayName.trim(),
                "bio" to bio.trim(),
                "musicUrl" to "", "avatarUrl" to "",
                "createdAt" to FieldValue.serverTimestamp()
            ))
        }.await()
        registerPush()
    }

    suspend fun updateProfile(name: String, bio: String, musicUrl: String) {
        require(name.trim().length in 2..50) { "Name must be 2–50 characters" }
        require(bio.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size <= 100) { "Maximum 100 words in bio" }
        require(musicUrl.isBlank() || musicUrl.startsWith("https://")) { "Music must have an HTTPS address" }
        db.collection("users").document(uid).update(
            mapOf("displayName" to name.trim(), "bio" to bio.trim(), "musicUrl" to musicUrl.trim())
        ).await()
    }

    suspend fun uploadMusic(uri: Uri): String {
        val ref = storage.reference.child("music/$uid/profile-${System.currentTimeMillis()}.mp3")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
    suspend fun uploadAvatar(uri: Uri): String {
        val ref = storage.reference.child("avatars/$uid/avatar-${System.currentTimeMillis()}.jpg")
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        db.collection("users").document(uid).update("avatarUrl", url).await()
        return url
    }

    suspend fun lookup(handle: String): Person? {
        val clean = handle.trim().removePrefix("@").lowercase()
        val id = db.collection("handles").document(clean).get().await().getString("uid") ?: return null
        return db.collection("users").document(id).get().await().person()
    }
    suspend fun getPerson(id: String): Person? = db.collection("users").document(id).get().await().person()

    suspend fun requestFriend(other: Person, me: Person) {
        require(other.uid != uid) { "You cannot add yourself" }
        val reqId = "${uid}_${other.uid}"
        db.collection("friendRequests").document(reqId).set(mapOf(
            "from" to uid, "to" to other.uid, "status" to "requested",
            "fromName" to me.displayName, "fromUsername" to me.username,
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
    }

    fun observeFriends(cb: (List<Friend>) -> Unit): ListenerRegistration =
        db.collection("users").document(uid).collection("friends")
            .addSnapshotListener { snap, _ -> cb(snap?.documents?.map { it.friend() } ?: emptyList()) }

    fun observeIncomingRequests(cb: (List<FriendRequest>) -> Unit): ListenerRegistration =
        db.collection("friendRequests").whereEqualTo("to", uid).whereEqualTo("status", "requested")
            .addSnapshotListener { snap, _ -> cb(snap?.documents?.map { it.request() } ?: emptyList()) }

    suspend fun acceptFriend(req: FriendRequest, me: Person) {
        val from = getPerson(req.from) ?: error("User not found")
        val batch = db.batch()
        batch.update(db.collection("friendRequests").document(req.id), "status", "accepted")
        batch.set(db.collection("users").document(uid).collection("friends").document(from.uid),
            mapOf("username" to from.username, "displayName" to from.displayName))
        batch.set(db.collection("users").document(from.uid).collection("friends").document(uid),
            mapOf("username" to me.username, "displayName" to me.displayName))
        batch.commit().await()
    }

    suspend fun ensureRoom(otherId: String): String {
        val id = roomId(uid, otherId)
        val doc = db.collection("rooms").document(id)
        // Rules reject reads of nonexistent rooms. Try existing get, then create if needed.
        val exists = try { doc.get().await().exists() } catch (_: Exception) { false }
        if (!exists) {
            try {
                doc.set(mapOf("members" to listOf(uid, otherId).sorted(),
                    "lastText" to "", "createdAt" to FieldValue.serverTimestamp())).await()
            } catch (createError: Exception) {
                // A friend may have created the same room concurrently.
                if (!doc.get().await().exists()) throw createError
            }
        }
        return id
    }

    fun observeMessages(room: String, cb: (List<Message>) -> Unit): ListenerRegistration =
        db.collection("rooms").document(room).collection("messages")
            .orderBy("at").limitToLast(150)
            .addSnapshotListener { snap, _ -> cb(snap?.documents?.map { it.message() } ?: emptyList()) }

    suspend fun sendMessage(room: String, text: String) {
        val value = text.trim()
        require(value.isNotEmpty() && value.length <= 4000) { "Message must be 1–4000 characters" }
        val batch = db.batch()
        val roomRef = db.collection("rooms").document(room)
        batch.set(roomRef.collection("messages").document(), mapOf(
            "sender" to uid, "text" to value, "at" to FieldValue.serverTimestamp()))
        batch.update(roomRef, mapOf("lastText" to value.take(100), "lastAt" to FieldValue.serverTimestamp()))
        batch.commit().await()
    }

    fun observeIncomingCalls(cb: (List<CallInfo>) -> Unit): ListenerRegistration =
        db.collection("calls").whereEqualTo("to", uid).whereEqualTo("status", "ringing")
            .addSnapshotListener { snap, _ -> cb(snap?.documents?.map { it.callInfo() } ?: emptyList()) }

    suspend fun createCall(other: Person, me: Person, lowData: Boolean): CallInfo {
        val call = db.collection("calls").document()
        val room = ensureRoom(other.uid)
        call.set(mapOf(
            "from" to uid, "to" to other.uid, "fromName" to me.displayName,
            "roomId" to room, "status" to "ringing", "lowData" to lowData,
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        return CallInfo(call.id, uid, other.uid, me.displayName, room, "ringing")
    }

    suspend fun acceptCall(call: CallInfo) {
        db.collection("calls").document(call.id).update("status", "accepted").await()
    }
    suspend fun declineCall(call: CallInfo) { db.collection("calls").document(call.id).update("status", "declined").await() }
    suspend fun endCall(callId: String) { db.collection("calls").document(callId).update("status", "ended").await() }

    /** Invite 2–3 accepted friends to a small group room (3–4 people including you). */
    suspend fun createGroupCall(friends: List<Friend>, me: Person, voiceOnly: Boolean): GroupCallInfo {
        require(friends.size in 2..3) { "Select two or three friends" }
        require(friends.map { it.uid }.distinct().size == friends.size) { "Each friend must be unique" }
        require(friends.none { it.uid == uid }) { "You cannot invite yourself" }
        // Verify current friendship on the server, not merely a stale UI selection.
        friends.forEach { friend ->
            require(db.collection("users").document(uid).collection("friends")
                .document(friend.uid).get().await().exists()) { "@${friend.username} is not a friend" }
        }
        val members = listOf(uid) + friends.map { it.uid }
        val names = mapOf(uid to me.displayName) + friends.associate { it.uid to it.displayName }
        val states = members.associateWith { if (it == uid) "joined" else "invited" }
        val ref = db.collection("groupCalls").document()
        ref.set(mapOf(
            "host" to uid, "hostName" to me.displayName, "members" to members,
            "names" to names, "memberStates" to states,
            "mode" to if (voiceOnly) "audio" else "video", "status" to "active",
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        return GroupCallInfo(ref.id, uid, me.displayName, members, names, states,
            if (voiceOnly) "audio" else "video", "active")
    }

    /** Invitee discovers invitations from rooms containing their UID. */
    fun observeGroupCalls(callback: (List<GroupCallInfo>) -> Unit): ListenerRegistration =
        db.collection("groupCalls").whereArrayContains("members", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) { callback(emptyList()); return@addSnapshotListener }
                callback(snap?.documents?.map { it.groupCallInfo() }
                    ?.filter { it.status == "active" } ?: emptyList())
            }

    fun observeGroupCall(callId: String, callback: (GroupCallInfo?) -> Unit): ListenerRegistration =
        db.collection("groupCalls").document(callId)
            .addSnapshotListener { doc, _ -> callback(if (doc?.exists() == true) doc.groupCallInfo() else null) }

    suspend fun acceptGroupCall(call: GroupCallInfo) {
        require(uid != call.host && call.memberStates[uid] == "invited") { "Invitation is no longer available" }
        db.collection("groupCalls").document(call.id)
            .update(com.google.firebase.firestore.FieldPath.of("memberStates", uid), "joined").await()
    }
    suspend fun declineGroupCall(call: GroupCallInfo) {
        db.collection("groupCalls").document(call.id)
            .update(com.google.firebase.firestore.FieldPath.of("memberStates", uid), "declined").await()
    }
    suspend fun leaveGroupCall(call: GroupCallInfo) {
        val ref = db.collection("groupCalls").document(call.id)
        if (call.host == uid) ref.update("status", "ended").await()
        else ref.update(com.google.firebase.firestore.FieldPath.of("memberStates", uid), "left").await()
    }

    suspend fun sendGroupReaction(callId: String, emoji: String) {
        require(emoji.length <= 12) { "Invalid reaction" }
        db.collection("groupCalls").document(callId).collection("reactions").add(mapOf(
            "sender" to uid, "emoji" to emoji, "at" to FieldValue.serverTimestamp()
        )).await()
    }
    fun observeGroupReactions(callId: String, cb: (List<String>) -> Unit): ListenerRegistration =
        db.collection("groupCalls").document(callId).collection("reactions")
            .orderBy("at").limitToLast(6)
            .addSnapshotListener { snap, _ ->
                cb(snap?.documents?.mapNotNull { it.getString("emoji") } ?: emptyList())
            }

    suspend fun sendReaction(callId: String, emoji: String) {
        db.collection("calls").document(callId).collection("reactions").add(mapOf(
            "sender" to uid, "emoji" to emoji, "at" to FieldValue.serverTimestamp()
        )).await()
    }
    fun observeReactions(callId: String, cb: (List<String>) -> Unit): ListenerRegistration =
        db.collection("calls").document(callId).collection("reactions")
            .orderBy("at").limitToLast(5)
            .addSnapshotListener { snap, _ -> cb(snap?.documents?.mapNotNull { it.getString("emoji") } ?: emptyList()) }

    suspend fun translate(text: String, language: String): String {
        if (text.isBlank()) return text
        val result = functions.getHttpsCallable("translateText")
            .call(mapOf("text" to text.take(4000), "target" to language)).await()
        @Suppress("UNCHECKED_CAST")
        return (result.getData() as? Map<String, Any>)?.get("translated") as? String ?: text
    }

    fun registerPush() {
        if (uid.isBlank()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val owner = uid
            if (owner.isNotBlank()) db.collection("pushTokens").document(owner)
                .set(mapOf("token" to token, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
        }
    }
}
