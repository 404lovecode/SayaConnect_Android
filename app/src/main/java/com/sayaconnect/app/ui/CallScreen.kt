package com.sayaconnect.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.firestore.ListenerRegistration
import com.sayaconnect.app.data.CallInfo
import com.sayaconnect.app.data.ConnectRepository
import com.sayaconnect.app.data.SettingsStore
import com.sayaconnect.app.rtc.RtcSession
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(repo: ConnectRepository, settings: SettingsStore, call: CallInfo, caller: Boolean,
               onClose: () -> Unit, onError: (Throwable) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var status by remember(call.id) { mutableStateOf("Connecting…") }
    var micOn by remember(call.id) { mutableStateOf(true) }
    var cameraOn by remember(call.id) { mutableStateOf(true) }
    var showEmojis by remember { mutableStateOf(false) }
    var reactions by remember(call.id) { mutableStateOf(emptyList<String>()) }
    val scope = rememberCoroutineScope()
    // Remember for this call only; all WebRTC objects are disposed after leaving this screen.
    val session = remember(call.id) {
        RtcSession(context, call.id, repo.uid, caller, settings.lowData,
            onStatus = { status = it }, onEnded = onClose)
    }
    DisposableEffect(session) {
        session.start()
        val reactionListener: ListenerRegistration = repo.observeReactions(call.id) { reactions = it }
        onDispose { reactionListener.remove(); session.dispose() }
    }
    Box(Modifier.fillMaxSize().background(wallpaperBrush("Midnight"))) {
        AndroidView(factory = { SurfaceViewRenderer(it).also(session::attachRemote) },
            modifier = Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(23.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SayaConnect  •  PRIVATE CALL", color = Color.White.copy(alpha = .75f),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(if (caller) "Calling…" else "In a call", color = Color.White,
                fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(status, color = Color.White.copy(alpha = .83f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(if (settings.lowData) "📶 Low data video" else "📹 Standard video",
                color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (reactions.isNotEmpty()) {
                Text(reactions.joinToString("  "), fontSize = 28.sp)
                Spacer(Modifier.height(14.dp))
            }
            if (showEmojis) {
                Row(Modifier.clip(RoundedCornerShape(25.dp)).background(Color(0xC9253152)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    listOf("❤️", "😂", "👏", "🔥", "🥰", "🙏").forEach { emoji ->
                        Text(emoji, modifier = Modifier.clickable {
                            showEmojis = false
                            scope.launch { try { repo.sendReaction(call.id, emoji) } catch (e: Exception) { onError(e) } }
                        }, fontSize = 26.sp)
                    }
                }
                Spacer(Modifier.height(13.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                CallControl(if (micOn) Icons.Default.Mic else Icons.Default.MicOff, "Microphone") {
                    micOn = session.toggleMic()
                }
                CallControl(if (cameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff, "Camera") {
                    cameraOn = session.toggleCamera()
                }
                CallControl(Icons.Default.Cameraswitch, "Flip camera") { session.switchCamera() }
                CallControl(Icons.Default.EmojiEmotions, "React") { showEmojis = !showEmojis }
            }
            Spacer(Modifier.height(22.dp))
            Button(onClick = {
                scope.launch { try { repo.endCall(call.id) } catch (e: Exception) { onError(e) } }
                onClose()
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE34A69)),
                modifier = Modifier.fillMaxWidth(.65f).height(53.dp)) {
                Icon(Icons.Default.CallEnd, null); Spacer(Modifier.width(9.dp))
                Text("End call", color = Color.White)
            }
            Spacer(Modifier.height(28.dp))
        }
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 19.dp, top = 80.dp)) {
            AndroidView(factory = { SurfaceViewRenderer(it).also(session::attachLocal) },
                modifier = Modifier.width(108.dp).height(158.dp).clip(RoundedCornerShape(17.dp)))
        }
    }
}

@Composable
private fun CallControl(image: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xBB33415F)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center) { Icon(image, label, tint = Color.White) }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color.White.copy(alpha = .85f), fontSize = 10.sp)
    }
}
