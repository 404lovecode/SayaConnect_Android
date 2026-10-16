package com.sayaconnect.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Person(
    val uid: String = "", val username: String = "", val displayName: String = "",
    val email: String = "", val bio: String = "", val musicUrl: String = "",
    val avatarUrl: String = ""
)
data class Friend(val uid: String, val username: String, val displayName: String)
data class FriendRequest(val id: String, val from: String, val to: String,
                         val fromName: String, val fromUsername: String, val status: String)
data class Message(val id: String, val sender: String, val text: String, val at: Timestamp?)
data class CallInfo(val id: String, val from: String, val to: String,
                    val fromName: String, val roomId: String, val status: String)

fun DocumentSnapshot.person(): Person? = if (!exists()) null else Person(
    id, getString("username").orEmpty(), getString("displayName").orEmpty(),
    getString("email").orEmpty(), getString("bio").orEmpty(),
    getString("musicUrl").orEmpty(), getString("avatarUrl").orEmpty()
)
fun DocumentSnapshot.message() = Message(id, getString("sender").orEmpty(),
    getString("text").orEmpty(), getTimestamp("at"))
fun DocumentSnapshot.friend() = Friend(id, getString("username").orEmpty(), getString("displayName").orEmpty())
fun DocumentSnapshot.request() = FriendRequest(id, getString("from").orEmpty(), getString("to").orEmpty(),
    getString("fromName").orEmpty(), getString("fromUsername").orEmpty(), getString("status").orEmpty())
fun DocumentSnapshot.callInfo() = CallInfo(id, getString("from").orEmpty(), getString("to").orEmpty(),
    getString("fromName").orEmpty(), getString("roomId").orEmpty(), getString("status").orEmpty())
fun roomId(a: String, b: String) = listOf(a, b).sorted().joinToString("_")

/** Group rooms use a P2P mesh capped at four users (host + three friends). */
data class GroupCallInfo(
    val id: String,
    val host: String,
    val hostName: String,
    val members: List<String>,
    val names: Map<String, String>,
    val memberStates: Map<String, String>,
    val mode: String,
    val status: String
)
fun DocumentSnapshot.groupCallInfo(): GroupCallInfo {
    @Suppress("UNCHECKED_CAST")
    val names = (get("names") as? Map<String, String>).orEmpty()
    @Suppress("UNCHECKED_CAST")
    val states = (get("memberStates") as? Map<String, String>).orEmpty()
    return GroupCallInfo(id, getString("host").orEmpty(), getString("hostName").orEmpty(),
        (get("members") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        names, states, getString("mode").orEmpty(), getString("status").orEmpty())
}
