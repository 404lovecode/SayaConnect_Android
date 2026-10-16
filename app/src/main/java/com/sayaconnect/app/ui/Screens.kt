package com.sayaconnect.app.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sayaconnect.app.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LoadingScreen() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
}

@Composable
fun PageTitle(title: String, subtitle: String = "", right: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        right?.invoke()
    }
}

@Composable
fun Avatar(name: String, image: String = "", size: Int = 54) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Accent.copy(alpha = .25f)),
        contentAlignment = Alignment.Center) {
        if (image.isNotBlank()) AsyncImage(model = image, contentDescription = name,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name.take(1).uppercase(), fontSize = (size / 2.5f).sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ContactRow(name: String, sub: String, avatar: String = "", onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Avatar(name, avatar)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

@Composable
fun AuthScreen(repo: ConnectRepository, ui: String, onError: (Throwable) -> Unit) {
    var mode by remember { mutableStateOf("phone") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var signUp by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(80.dp).background(Accent, RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Forum, null, Modifier.size(42.dp), tint = Color.White)
        }
        Spacer(Modifier.height(15.dp))
        Text("SayaConnect", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text("Connect hearts, beyond borders 🌏", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(22.dp))
        Text(if (ui == "my") "ဝင်ရောက်ရန် နည်းလမ်းရွေးပါ" else "Choose how to sign in",
            fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(Triple("phone", "📱", if (ui == "my") "ဖုန်း" else "Phone"),
                Triple("email", "✉️", if (ui == "my") "အီးမေးလ်" else "Email"),
                Triple("guest", "👤", if (ui == "my") "ဧည့်သည်" else "Guest")
            ).forEach { (id, emoji, label) ->
                FilterChip(selected = mode == id, onClick = { mode = id },
                    label = { Text("$emoji $label", maxLines = 1) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        when (mode) {
            "phone" -> PhoneAuthForm(repo, ui, linkToCurrent = false, onFinished = {}, onError = onError)
            "email" -> {
                Text(if (signUp) t("Create account", ui) else t("Sign in", ui),
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(t("Email address", ui)) }, leadingIcon = { Icon(Icons.Default.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(pass, { pass = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(t("Password", ui)) }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(14.dp))
                Button(onClick = {
                    working = true
                    scope.launch {
                        try { if (signUp) repo.signUp(email, pass) else repo.login(email, pass) }
                        catch (e: Exception) { onError(e) }
                        finally { working = false }
                    }
                }, enabled = !working && email.isNotBlank() && pass.length >= 6,
                    modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (working) CircularProgressIndicator(Modifier.size(19.dp))
                    else Text(if (signUp) t("Create account", ui) else t("Sign in", ui))
                }
                TextButton(onClick = { signUp = !signUp }) {
                    Text(if (signUp) "Already have an account? Sign in" else "New here? Create an account")
                }
            }
            "guest" -> {
                Text(if (ui == "my") "အီးမေးလ်နဲ့ ဖုန်းနံပါတ် မလိုပါ" else "No email or phone number required",
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(if (ui == "my")
                    "ဧည့်သည်အကောင့်သည် ယာယီဖြစ်သည်။ ဖုန်းပြောင်းမီ သို့မဟုတ် ထွက်မီ ဖုန်းနံပါတ်ချိတ်ဆက်ပါ။"
                    else "Guest accounts are temporary. Link your phone number before signing out, uninstalling, or changing phones or you may lose the account.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(15.dp))
                Button(onClick = {
                    working = true
                    scope.launch {
                        try { repo.loginAsGuest() } catch (e: Exception) { onError(e) }
                        finally { working = false }
                    }
                }, enabled = !working, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (working) CircularProgressIndicator(Modifier.size(19.dp))
                    else Text(if (ui == "my") "ဧည့်သည်အဖြစ် ဝင်မည်" else "Continue as guest")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(if (ui == "my") "အကောင့်ဖွင့်ပြီးရင် ကိုယ်ပိုင် username သတ်မှတ်နိုင်ပါတယ်။"
            else "Choose a unique username after signing in. Phone verification needs SMS delivery.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CreateProfileScreen(repo: ConnectRepository, ui: String, onError: (Throwable) -> Unit) {
    var handle by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(25.dp),
        verticalArrangement = Arrangement.Center) {
        Text("✨ One last thing", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Create your global identity on SayaConnect",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(value = handle, onValueChange = { handle = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
            label = { Text(t("Username", ui)) }, prefix = { Text("@") }, modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("3–20: a-z, 0-9 and _ (unique)") })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(name, { name = it.take(50) }, label = { Text(t("Display name", ui)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(bio, { bio = it }, label = { Text(t("Bio", ui)) }, modifier = Modifier.fillMaxWidth(), minLines = 3,
            supportingText = { Text("${bio.trim().split(Regex("\\s+")).count { it.isNotBlank() }} / 100 words") })
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            loading = true
            scope.launch {
                try { repo.createProfile(handle, name, bio) }
                catch (e: Exception) { onError(e) }
                finally { loading = false }
            }
        }, enabled = !loading && handle.length >= 3 && name.length >= 2,
            modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(t("Continue", ui)) }
        TextButton(onClick = { repo.logout() }) { Text("Switch account") }
    }
}

@Composable
fun ChatsHome(friends: List<Friend>, settings: SettingsStore, onChat: (Friend) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageTitle(t("Chats", settings.uiLanguage), "Your people, one tap away", right = {
            Icon(Icons.Default.Forum, null, tint = MaterialTheme.colorScheme.primary)
        })
        AccentPanel(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("🌏 STAY CLOSE, FROM ANYWHERE", color = Color.White.copy(alpha = .75f),
                fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            Spacer(Modifier.height(7.dp))
            Text("Every conversation brings us closer.", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 22.sp)
            Spacer(Modifier.height(5.dp))
            Text("Myanmar ↔ the world · Audio-friendly low data calls",
                fontSize = 12.sp, color = Color.White.copy(alpha = .85f))
        }
        Spacer(Modifier.height(14.dp))
        Text("CONVERSATIONS  ·  ${friends.size}", modifier = Modifier.padding(20.dp),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        if (friends.isEmpty()) EmptyState("💬", t("No chats yet", settings.uiLanguage), "Add a friend from Explore to start talking")
        else LazyColumn {
            items(friends, key = { it.uid }) { friend ->
                ContactRow(friend.displayName, "@${friend.username} · Tap to chat", onClick = { onChat(friend) }) {
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(35.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 46.sp)
        Spacer(Modifier.height(9.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun FriendsScreen(friends: List<Friend>, requests: List<FriendRequest>, repo: ConnectRepository, me: Person,
                  ui: String, onChat: (Friend) -> Unit, onGroupCall: () -> Unit,
                  onError: (Throwable) -> Unit) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        PageTitle(t("Friends", ui), "${friends.size} connections · ${requests.size} pending",
            right = { FilledTonalButton(onClick = onGroupCall, enabled = friends.size >= 2) {
                Icon(Icons.Default.Groups, null)
                Spacer(Modifier.width(6.dp))
                Text(t("Group call", ui))
            } })
        if (requests.isNotEmpty()) {
            Text(t("Friend requests", ui), modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                fontWeight = FontWeight.Bold)
            requests.forEach { request ->
                ContactRow(request.fromName, "@${request.fromUsername}", onClick = {}) {
                    Button(onClick = {
                        scope.launch { try { repo.acceptFriend(request, me) } catch (e: Exception) { onError(e) } }
                    }) { Text(t("Accept", ui)) }
                }
            }
            HorizontalDivider()
        }
        if (friends.isEmpty()) EmptyState("🤝", t("No friends yet", ui), "Search for someone by their exact @username")
        else LazyColumn { items(friends, key = { it.uid }) { friend ->
            ContactRow(friend.displayName, "@${friend.username}", onClick = { onChat(friend) }) {
                Icon(Icons.Default.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary)
            }
        } }
    }
}

@Composable
fun ExploreScreen(repo: ConnectRepository, me: Person, ui: String, onError: (Throwable) -> Unit) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Person?>(null) }
    var searching by remember { mutableStateOf(false) }
    var requested by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    val musicPlayer = remember { MediaPlayer() }
    DisposableEffect(musicPlayer) { onDispose { musicPlayer.release() } }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        PageTitle(t("Explore", ui), "Find your people by username")
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, { query = it; result = null; requested = false },
                modifier = Modifier.weight(1f), singleLine = true,
                label = { Text(t("Search username", ui)) }, leadingIcon = { Text("@") })
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = {
                scope.launch {
                    searching = true
                    try { result = repo.lookup(query) }
                    catch (e: Exception) { onError(e) }
                    finally { searching = false }
                }
            }, enabled = !searching && query.isNotBlank()) { Icon(Icons.Default.Search, null) }
        }
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(20.dp))
        if (!searching && query.isNotBlank() && result == null)
            Text("Search using the button to find an exact username",
                modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        result?.let { person ->
            ElevatedCard(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(person.displayName, person.avatarUrl, 69)
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text(person.displayName, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("@${person.username}")
                        if (person.bio.isNotBlank()) Text(person.bio, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (person.musicUrl.isNotBlank()) {
                    TextButton(onClick = {
                        try {
                            if (listening) { musicPlayer.stop(); musicPlayer.reset(); listening = false }
                            else {
                                musicPlayer.reset()
                                musicPlayer.setDataSource(person.musicUrl)
                                musicPlayer.setOnPreparedListener { it.start(); listening = true }
                                musicPlayer.setOnCompletionListener { listening = false }
                                musicPlayer.prepareAsync()
                            }
                        } catch (e: Exception) { onError(e) }
                    }, modifier = Modifier.padding(start = 18.dp)) {
                        Icon(if (listening) Icons.Default.Stop else Icons.Default.MusicNote, null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (listening) "Stop song" else "Listen to profile music")
                    }
                }
                Button(onClick = {
                    scope.launch {
                        try { repo.requestFriend(person, me); requested = true }
                        catch (e: Exception) { onError(e) }
                    }
                }, modifier = Modifier.fillMaxWidth().padding(start = 19.dp, end = 19.dp, bottom = 19.dp),
                    enabled = !requested && person.uid != me.uid) {
                    Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp))
                    Text(if (person.uid == me.uid) "This is you" else if (requested) "Request sent ✓" else t("Add friend", ui))
                }
            }
        }
    }
}

@Composable
fun ChatScreen(repo: ConnectRepository, settings: SettingsStore, me: Person, other: Person,
               onBack: () -> Unit, onCall: () -> Unit, onError: (Throwable) -> Unit) {
    var room by remember(other.uid) { mutableStateOf("") }
    var messages by remember(other.uid) { mutableStateOf(emptyList<Message>()) }
    var draft by remember { mutableStateOf("") }
    var emojiMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    LaunchedEffect(other.uid) {
        try { room = repo.ensureRoom(other.uid) } catch (e: Exception) { onError(e) }
    }
    DisposableEffect(room) {
        if (room.isBlank()) onDispose { }
        else { val s = repo.observeMessages(room) { messages = it }; onDispose { s.remove() } }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) state.animateScrollToItem(messages.lastIndex) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Avatar(other.displayName, other.avatarUrl, 42)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(other.displayName, fontWeight = FontWeight.Bold)
                Text("@${other.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onCall) { Icon(Icons.Default.Videocam, "Video call", tint = MaterialTheme.colorScheme.primary) }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize().background(wallpaperBrush(settings.wallpaper)))
            if (settings.customWallpaper.isNotBlank()) AsyncImage(
                model = settings.customWallpaper, contentDescription = "Chat wallpaper",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
            LazyColumn(Modifier.fillMaxSize().padding(10.dp), state = state,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg, msg.sender == me.uid, repo, settings)
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp),
            verticalAlignment = Alignment.Bottom) {
            Box {
                IconButton(onClick = { emojiMenu = true }) { Icon(Icons.Default.EmojiEmotions, "Emoji") }
                DropdownMenu(expanded = emojiMenu, onDismissRequest = { emojiMenu = false }) {
                    listOf("😊", "❤️", "😂", "🙏", "🔥", "😍", "🥰", "👍").forEach { emoji ->
                        DropdownMenuItem(text = { Text(emoji, fontSize = 23.sp) }, onClick = {
                            draft += emoji; emojiMenu = false
                        })
                    }
                }
            }
            OutlinedTextField(draft, { draft = it.take(4000) }, modifier = Modifier.weight(1f),
                maxLines = 4, placeholder = { Text(t("Type a message", settings.uiLanguage)) },
                shape = RoundedCornerShape(25.dp))
            IconButton(onClick = {
                val text = draft; draft = ""
                scope.launch { try { repo.sendMessage(room, text) } catch (e: Exception) { onError(e); draft = text } }
            }, enabled = room.isNotBlank() && draft.isNotBlank()) {
                Icon(Icons.Default.Send, t("Send", settings.uiLanguage), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, mine: Boolean, repo: ConnectRepository, settings: SettingsStore) {
    var translated by remember(message.id, settings.targetLanguage, settings.autoTranslate) { mutableStateOf<String?>(null) }
    var failed by remember(message.id, settings.targetLanguage, settings.autoTranslate) { mutableStateOf(false) }
    LaunchedEffect(message.id, settings.targetLanguage, settings.autoTranslate) {
        if (!mine && settings.autoTranslate) {
            try { translated = repo.translate(message.text, settings.targetLanguage) }
            catch (_: Exception) { failed = true }
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Column(Modifier.fillMaxWidth(.83f)
            .background(if (mine) Color(0xFF6553CE) else Color(0xDF1C2942), RoundedCornerShape(19.dp))
            .padding(horizontal = 15.dp, vertical = 11.dp)) {
            Text(translated ?: message.text, color = Color.White, fontSize = 15.sp)
            if (translated != null && translated != message.text) {
                Spacer(Modifier.height(7.dp))
                Text("Original: ${message.text}", color = Color.White.copy(alpha = .68f), fontSize = 11.sp)
            }
            if (failed) Text("Translation unavailable · showing original", color = Color(0xFFFFDA9D), fontSize = 10.sp)
            Spacer(Modifier.height(3.dp))
            val whenSent = message.at?.toDate()?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "…"
            Text(whenSent, color = Color.White.copy(alpha = .65f), fontSize = 10.sp)
        }
    }
}

@Composable
fun ProfileScreen(repo: ConnectRepository, me: Person, ui: String, onError: (Throwable) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var name by remember(me.displayName) { mutableStateOf(me.displayName) }
    var bio by remember(me.bio) { mutableStateOf(me.bio) }
    var musicUrl by remember(me.musicUrl) { mutableStateOf(me.musicUrl) }
    var uploading by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val player = remember { MediaPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            uploading = true
            try { musicUrl = repo.uploadMusic(uri); repo.updateProfile(name, bio, musicUrl) }
            catch (e: Exception) { onError(e) }
            finally { uploading = false }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            uploading = true
            try { repo.uploadAvatar(uri) } catch (e: Exception) { onError(e) }
            finally { uploading = false }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle(t("Profile", ui), "The person behind the username")
        AccentPanel(Modifier.fillMaxWidth().padding(horizontal = 19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(me.displayName, me.avatarUrl, 78)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(me.displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("@${me.username}", color = Color.White.copy(alpha = .85f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(me.bio.ifBlank { "Add a bio and tell your story ✨" }, color = Color.White.copy(alpha = .9f))
            Spacer(Modifier.height(12.dp))
            Text("🎶  ${if (me.musicUrl.isBlank()) "Add your profile song" else "Profile music added"}",
                fontSize = 13.sp, color = Color.White)
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { editing = !editing }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Edit, null); Spacer(Modifier.width(7.dp)); Text(t("Edit profile", ui))
            }
            OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(7.dp)); Text(t("Upload photo", ui))
            }
        }
        if (editing) {
            Column(Modifier.padding(20.dp)) {
                OutlinedTextField(name, { name = it.take(50) }, label = { Text(t("Display name", ui)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(bio, { bio = it }, label = { Text(t("Bio", ui)) }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                    supportingText = { Text("${bio.trim().split(Regex("\\s+")).count { it.isNotBlank() }} / 100 words") })
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(musicUrl, { musicUrl = it }, label = { Text("Profile music HTTPS URL") },
                    modifier = Modifier.fillMaxWidth(), supportingText = { Text("Or upload MP3/M4A from your phone") })
                Spacer(Modifier.height(9.dp))
                Button(onClick = {
                    scope.launch {
                        try { repo.updateProfile(name, bio, musicUrl); editing = false }
                        catch (e: Exception) { onError(e) }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(t("Save", ui)) }
            }
        }
        ListItem(headlineContent = { Text(t("Music", ui), fontWeight = FontWeight.Bold) },
            supportingContent = { Text(if (me.musicUrl.isBlank()) "Personalize your profile with a song" else "Song ready to play") },
            leadingContent = { Icon(Icons.Default.MusicNote, null) }, trailingContent = {
                IconButton(onClick = { musicPicker.launch("audio/*") }) { Icon(Icons.Default.FileUpload, "Upload music") }
            })
        if (uploading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(15.dp))
        if (me.musicUrl.isNotBlank()) {
            TextButton(onClick = {
                try {
                    if (playing) { player.stop(); player.reset(); playing = false }
                    else {
                        player.reset(); player.setDataSource(me.musicUrl)
                        player.setOnPreparedListener { it.start(); playing = true }
                        player.setOnCompletionListener { playing = false }
                        player.prepareAsync()
                    }
                } catch (e: Exception) { onError(e) }
            }, modifier = Modifier.padding(start = 22.dp)) {
                Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(5.dp))
                Text(if (playing) "Stop profile music" else "Play profile music")
            }
        }
        Text("Only share music you have permission to use.", modifier = Modifier.padding(22.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsScreen(repo: ConnectRepository, settings: SettingsStore, onLinkPhone: () -> Unit) {
    val context = LocalContext.current
    val ui = settings.uiLanguage
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: Exception) { }
            settings.setCustomWallpaper(uri.toString())
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle(t("Settings", ui), "Make SayaConnect feel like you")
        SectionLabel(t("App language", ui))
        OptionSelect(listOf("en" to "English", "my" to "မြန်မာ"), settings.uiLanguage, settings::setUiLanguage)
        SectionLabel(t("Translation language", ui))
        OptionSelect(languages.toList(), settings.targetLanguage, settings::setTargetLanguage)
        SwitchSetting(t("Auto translate", ui), "Translate received chat messages into your chosen language",
            settings.autoTranslate, settings::setAutoTranslate)
        SectionLabel(t("Appearance", ui))
        OptionSelect(listOf("system" to t("System", ui), "light" to t("Light", ui),
            "dark" to t("Dark", ui)), settings.darkMode, settings::setDarkMode)
        SectionLabel(t("Wallpaper", ui))
        OptionSelect(listOf("Aurora", "Ocean", "Rose", "Midnight", "Mint").map { it to it },
            settings.wallpaper) { settings.setWallpaper(it); settings.setCustomWallpaper("") }
        OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.padding(start = 20.dp)) {
            Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Choose photo wallpaper")
        }
        if (settings.customWallpaper.isNotBlank()) TextButton(onClick = { settings.setCustomWallpaper("") },
            modifier = Modifier.padding(start = 20.dp)) { Text("Remove custom wallpaper") }
        SectionLabel("Network & calling")
        SwitchSetting(t("Low data mode", ui), "Start calls at 320×240, 10 fps and ~150 kbps video",
            settings.lowData, settings::setLowData)
        Text("WebRTC works on Wi-Fi and mobile data. For restricted networks and difficult NAT, configure a TURN relay. No app can promise to bypass ISP blocks without a VPN.",
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        SectionLabel(if (ui == "my") "အကောင့် လုံခြုံရေး" else "Account security")
        val isGuest = repo.auth.currentUser?.isAnonymous == true
        val hasPhone = !repo.auth.currentUser?.phoneNumber.isNullOrBlank()
        if (isGuest) Text(
            if (ui == "my") "သင်သည် ဧည့်သည်အကောင့် အသုံးပြုနေသည်။ ဖုန်းနံပါတ်မချိတ်ဘဲ ထွက်လျှင် အကောင့်ဆုံးရှုံးနိုင်သည်။"
            else "You are using a temporary guest account. Link a phone number before you sign out or switch devices.",
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.error)
        if (!hasPhone) {
            OutlinedButton(onClick = onLinkPhone, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Icon(Icons.Default.Smartphone, null)
                Spacer(Modifier.width(7.dp))
                Text(if (ui == "my") "ဖုန်းနံပါတ် ချိတ်ဆက်ရန်" else "Link phone number to this account")
            }
        }
        var showSignOutWarning by remember { mutableStateOf(false) }
        if (showSignOutWarning) AlertDialog(onDismissRequest = { showSignOutWarning = false },
            title = { Text(if (ui == "my") "အကောင့်ဆုံးရှုံးနိုင်သည်" else "Temporary account warning") },
            text = { Text(if (ui == "my")
                "ဧည့်သည်အကောင့်ကို ဖုန်းနံပါတ်မချိတ်ဘဲ ထွက်လျှင် ပြန်ဝင်နိုင်မည် မဟုတ်ပါ။"
                else "Signing out of a guest account without linking a phone number can permanently lose your profile, friends, and messages.") },
            confirmButton = { TextButton(onClick = { showSignOutWarning = false; repo.logout() }) {
                Text(if (ui == "my") "ထွက်မည်" else "Sign out anyway") } },
            dismissButton = { TextButton(onClick = { showSignOutWarning = false }) {
                Text(if (ui == "my") "မထွက်တော့ပါ" else "Cancel") } })
        TextButton(onClick = { if (isGuest) showSignOutWarning = true else repo.logout() },
            modifier = Modifier.padding(20.dp)) {
            Icon(Icons.Default.Logout, null); Spacer(Modifier.width(7.dp)); Text(t("Sign out", ui))
        }
        Text("SayaConnect 1.1 · Email-free sign-in source edition",
            modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label.uppercase(), modifier = Modifier.padding(start = 23.dp, top = 15.dp, bottom = 5.dp),
        color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SwitchSetting(label: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun OptionSelect(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)) {
        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(options.firstOrNull { it.first == selected }?.second ?: selected, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
