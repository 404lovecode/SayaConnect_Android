package com.sayaconnect.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseUser
import com.sayaconnect.app.data.*
import com.sayaconnect.app.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val repo = remember { ConnectRepository() }
            val settings = remember { SettingsStore(applicationContext) }
            val dark = when (settings.darkMode) {
                "dark" -> true; "light" -> false; else -> isSystemInDarkTheme()
            }
            SayaTheme(dark) { ConnectApp(repo, settings) }
        }
    }
}

@Composable
private fun ConnectApp(repo: ConnectRepository, settings: SettingsStore) {
    var user by remember { mutableStateOf<FirebaseUser?>(repo.auth.currentUser) }
    var me by remember { mutableStateOf<Person?>(null) }
    var checkingProfile by remember { mutableStateOf(user != null) }
    var tab by remember { mutableStateOf("Chats") }
    var chatWith by remember { mutableStateOf<Person?>(null) }
    var activeCall by remember { mutableStateOf<CallInfo?>(null) }
    var activeGroup by remember { mutableStateOf<GroupCallInfo?>(null) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showPhoneLink by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf(emptyList<Friend>()) }
    var requests by remember { mutableStateOf(emptyList<FriendRequest>()) }
    var incomingCalls by remember { mutableStateOf(emptyList<CallInfo>()) }
    var groupCalls by remember { mutableStateOf(emptyList<GroupCallInfo>()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun showError(t: Throwable) { scope.launch { snackbar.showSnackbar(t.message ?: "Something went wrong") } }
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) afterPermission?.invoke()
        else scope.launch { snackbar.showSnackbar("Allow camera and microphone permission for calls") }
        afterPermission = null
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun withPermissions(needCamera: Boolean = true, action: () -> Unit) {
        afterPermission = action
        permissionsLauncher.launch(if (needCamera)
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            else arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    DisposableEffect(repo) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            val previousUid = user?.uid
            val signedIn = auth.currentUser
            user = signedIn
            if (signedIn == null) {
                me = null; chatWith = null; activeCall = null; activeGroup = null
                showPhoneLink = false; checkingProfile = false
            } else if (previousUid != signedIn.uid) {
                // A provider link on the SAME user must not restart profile loading.
                me = null
                checkingProfile = true
            }
        }
        repo.auth.addAuthStateListener(listener)
        onDispose { repo.auth.removeAuthStateListener(listener) }
    }
    DisposableEffect(user?.uid) {
        val id = user?.uid
        if (id == null) { checkingProfile = false; onDispose { } }
        else {
            val subscription = repo.observeMe(id) { profile ->
                me = profile; checkingProfile = false
                if (profile != null) repo.registerPush()
            }
            onDispose { subscription.remove() }
        }
    }
    DisposableEffect(me?.uid) {
        if (me == null) onDispose { }
        else {
            val a = repo.observeFriends { friends = it }
            val b = repo.observeIncomingRequests { requests = it }
            val c = repo.observeIncomingCalls { incomingCalls = it }
            val d = repo.observeGroupCalls { groupCalls = it }
            if (Build.VERSION.SDK_INT >= 33)
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            onDispose { a.remove(); b.remove(); c.remove(); d.remove() }
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            user == null -> AuthScreen(repo, settings.uiLanguage, ::showError)
            checkingProfile -> LoadingScreen()
            me == null -> CreateProfileScreen(repo, settings.uiLanguage, ::showError)
            showPhoneLink -> PhoneLinkScreen(repo, settings.uiLanguage,
                onBack = { showPhoneLink = false }, onFinished = { showPhoneLink = false }, onError = ::showError)
            activeGroup != null -> {
                GroupCallScreen(repo, settings, activeGroup!!,
                    onClose = { activeGroup = null }, onError = ::showError)
            }
            activeCall != null -> {
                val call = activeCall!!
                CallScreen(repo, settings, call, call.from == repo.uid,
                    onClose = { activeCall = null }, onError = ::showError)
            }
            chatWith != null -> {
                val other = chatWith!!
                ChatScreen(repo, settings, me!!, other,
                    onBack = { chatWith = null }, onCall = {
                        withPermissions {
                            scope.launch {
                                try { activeCall = repo.createCall(other, me!!, settings.lowData) }
                                catch (e: Exception) { showError(e) }
                            }
                        }
                    }, onError = ::showError)
            }
            else -> Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf("Chats", "Friends", "Explore", "Profile", "Settings").forEach { item ->
                            val icon = when (item) {
                                "Chats" -> Icons.Default.ChatBubbleOutline
                                "Friends" -> Icons.Default.PeopleOutline
                                "Explore" -> Icons.Default.TravelExplore
                                "Profile" -> Icons.Default.PersonOutline
                                else -> Icons.Default.Settings
                            }
                            NavigationBarItem(selected = tab == item, onClick = { tab = item },
                                icon = { Icon(icon, null) },
                                label = { Text(t(item, settings.uiLanguage), maxLines = 1) })
                        }
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    when (tab) {
                        "Chats" -> ChatsHome(friends, settings, onChat = { friend ->
                            scope.launch {
                                try { chatWith = repo.getPerson(friend.uid) ?: throw IllegalStateException("Friend not found") }
                                catch (e: Exception) { showError(e) }
                            }
                        })
                        "Friends" -> FriendsScreen(friends, requests, repo, me!!, settings.uiLanguage,
                            onChat = { friend ->
                                scope.launch {
                                    try { chatWith = repo.getPerson(friend.uid) }
                                    catch (e: Exception) { showError(e) }
                                }
                            }, onGroupCall = { showGroupPicker = true }, onError = ::showError)
                        "Explore" -> ExploreScreen(repo, me!!, settings.uiLanguage, ::showError)
                        "Profile" -> ProfileScreen(repo, me!!, settings.uiLanguage, ::showError)
                        "Settings" -> SettingsScreen(repo, settings, onLinkPhone = { showPhoneLink = true })
                    }
                }

            }
        }
        if (activeCall == null && activeGroup == null && !showPhoneLink && user != null && me != null) {
            if (showGroupPicker) {
                GroupCallPicker(friends, settings.uiLanguage,
                    onDismiss = { showGroupPicker = false },
                    onStart = { invitees, voiceOnly ->
                        showGroupPicker = false
                        withPermissions(needCamera = !voiceOnly) {
                            scope.launch {
                                try { activeGroup = repo.createGroupCall(invitees, me!!, voiceOnly) }
                                catch (e: Exception) { showError(e) }
                            }
                        }
                    })
            }
            if (!showGroupPicker) incomingCalls.firstOrNull { it.from != repo.uid }?.let { incoming ->
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(t("Incoming call", settings.uiLanguage)) },
                            text = { Text("${incoming.fromName} is calling you 🎥") },
                            confirmButton = {
                                Button(onClick = {
                                    withPermissions {
                                        scope.launch {
                                            try { repo.acceptCall(incoming); activeCall = incoming }
                                            catch (e: Exception) { showError(e) }
                                        }
                                    }
                                }) { Text(t("Answer", settings.uiLanguage)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { scope.launch { try { repo.declineCall(incoming) } catch (e: Exception) { showError(e) } } }) {
                                    Text(t("Decline", settings.uiLanguage))
                                }
                            }
                        )
                    }
            if (incomingCalls.none { it.from != repo.uid } && !showGroupPicker) {
                groupCalls.firstOrNull {
                    it.host != repo.uid && it.memberStates[repo.uid] == "invited"
                }?.let { invitation ->
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(if (settings.uiLanguage == "my")
                            "အဖွဲ့လိုက် ဖုန်းခေါ်ဆိုမှု" else "Incoming group call") },
                        text = { Text("${invitation.hostName} invited you to a group " +
                            (if (invitation.mode == "audio") "voice" else "video") + " call " +
                            "(${invitation.members.size} people)") },
                        confirmButton = {
                            Button(onClick = {
                                withPermissions(needCamera = invitation.mode != "audio") {
                                    scope.launch {
                                        try {
                                            repo.acceptGroupCall(invitation)
                                            activeGroup = invitation.copy(
                                                memberStates = invitation.memberStates + (repo.uid to "joined"))
                                        } catch (e: Exception) { showError(e) }
                                    }
                                }
                            }) { Text(t("Answer", settings.uiLanguage)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                scope.launch { try { repo.declineGroupCall(invitation) }
                                catch (e: Exception) { showError(e) } }
                            }) { Text(t("Decline", settings.uiLanguage)) }
                        }
                    )
                }
            }
        }
        if (activeCall != null || activeGroup != null || showPhoneLink || chatWith != null || user == null || me == null) {
            SnackbarHost(snackbar, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
        }
    }
}
