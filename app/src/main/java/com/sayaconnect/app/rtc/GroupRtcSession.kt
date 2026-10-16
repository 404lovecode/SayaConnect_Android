package com.sayaconnect.app.rtc

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sayaconnect.app.BuildConfig
import org.webrtc.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Small-room WebRTC mesh (up to FOUR total participants). Every remote attendee has
 * a distinct PeerConnection but ALL connections share one camera, microphone and EGL.
 * Firebase is ONLY the signaling plane. WebRTC carries the actual encrypted media.
 * This is an MVP: larger meetings should use an SFU, not a P2P mesh.
 */
class GroupRtcSession(
    context: Context,
    private val groupId: String,
    private val myUid: String,
    private val lowData: Boolean,
    private val voiceOnly: Boolean,
    private val onPeerStatus: (String, String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val callRef = FirebaseFirestore.getInstance().collection("groupCalls").document(groupId)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val originalMode = audioManager.mode
    @Suppress("DEPRECATION") private val originalSpeaker = audioManager.isSpeakerphoneOn
    val egl: EglBase = EglBase.create()
    private val factory: PeerConnectionFactory
    private val audioSource: AudioSource
    private val audioTrack: AudioTrack
    private val videoSource: VideoSource?
    private val videoTrack: VideoTrack?
    private val helper: SurfaceTextureHelper?
    private val capturer: CameraVideoCapturer?
    private val peers = ConcurrentHashMap<String, RemotePeer>()
    private var localRenderer: SurfaceViewRenderer? = null
    @Volatile private var ended = false
    var micEnabled = true; private set
    var cameraEnabled = !voiceOnly; private set

    private class RemotePeer(
        val uid: String,
        val pc: PeerConnection,
        val pairListener: ListenerRegistration,
        val candidateListener: ListenerRegistration,
        val pending: CopyOnWriteArrayList<IceCandidate>,
        var remoteTrack: VideoTrack? = null,
        var renderer: SurfaceViewRenderer? = null,
        var remoteSdpSet: Boolean = false,
        var offerSeen: Boolean = false,
        var answerSeen: Boolean = false
    )

    init {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = true
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext).createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .createPeerConnectionFactory()
        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("group-audio", audioSource)

        if (!voiceOnly) {
            val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(context))
                Camera2Enumerator(context) else Camera1Enumerator(false)
            val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()
            capturer = cameraName?.let { enumerator.createCapturer(it, null) as? CameraVideoCapturer }
            if (capturer != null) {
                helper = SurfaceTextureHelper.create("GroupCamera", egl.eglBaseContext)
                videoSource = factory.createVideoSource(false)
                capturer.initialize(helper, context.applicationContext, videoSource.capturerObserver)
                capturer.startCapture(if (lowData) 160 else 480, if (lowData) 120 else 270,
                    if (lowData) 8 else 15)
                videoTrack = factory.createVideoTrack("group-video", videoSource)
            } else {
                helper = null; videoSource = null; videoTrack = null; cameraEnabled = false
            }
        } else {
            capturer = null; helper = null; videoSource = null; videoTrack = null
        }
    }

    fun attachLocal(view: SurfaceViewRenderer) {
        view.init(egl.eglBaseContext, null)
        view.setMirror(true)
        localRenderer = view
        videoTrack?.addSink(view)
    }

    fun detachLocal(view: SurfaceViewRenderer) {
        if (!ended) videoTrack?.removeSink(view)
        if (localRenderer === view) localRenderer = null
        view.release()
    }

    fun attachRemote(uid: String, view: SurfaceViewRenderer) {
        view.init(egl.eglBaseContext, null)
        view.setEnableHardwareScaler(true)
        peers[uid]?.let { peer ->
            peer.renderer = view
            peer.remoteTrack?.addSink(view)
        }
    }

    fun detachRemote(uid: String, view: SurfaceViewRenderer) {
        if (!ended) peers[uid]?.let { peer ->
            peer.remoteTrack?.removeSink(view)
            if (peer.renderer === view) peer.renderer = null
        }
        view.release()
    }

    /** Called for each snapshot of the room. Once someone leaves, close that link. */
    fun syncJoined(joinedUids: Set<String>) {
        if (ended) return
        val remotes = joinedUids.filter { it != myUid }.toSet()
        peers.keys.toList().filter { it !in remotes }.forEach(::removePeer)
        remotes.filter { !peers.containsKey(it) }.forEach(::createPeer)
    }

    private fun createPeer(remoteUid: String) {
        if (ended || peers.containsKey(remoteUid)) return
        val initiator = myUid < remoteUid
        val a = if (initiator) myUid else remoteUid
        val b = if (initiator) remoteUid else myUid
        val pairRef = callRef.collection("pairs").document("${a}_${b}")
        val config = PeerConnection.RTCConfiguration(buildList {
            add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
            add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer())
            if (BuildConfig.TURN_URL.isNotBlank()) {
                add(PeerConnection.IceServer.builder(BuildConfig.TURN_URL)
                    .setUsername(BuildConfig.TURN_USER).setPassword(BuildConfig.TURN_PASSWORD)
                    .createIceServer())
            }
        }).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        val pc = factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (!ended) main.post {
                    onPeerStatus(remoteUid, when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> "Connected"
                        PeerConnection.IceConnectionState.DISCONNECTED -> "Reconnecting"
                        PeerConnection.IceConnectionState.FAILED -> "Connection failed (TURN needed?)"
                        else -> "Connecting"
                    })
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null || ended || !peers.containsKey(remoteUid)) return
                pairRef.collection("candidates").add(mapOf(
                    "sender" to myUid, "candidate" to candidate.sdp,
                    "sdpMid" to (candidate.sdpMid ?: ""),
                    "sdpMLineIndex" to candidate.sdpMLineIndex
                )).addOnFailureListener { report("ICE write: ${it.message}") }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                stream?.videoTracks?.firstOrNull()?.let { receiveTrack(remoteUid, it) }
            }
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                (receiver?.track() as? VideoTrack)?.let { receiveTrack(remoteUid, it) }
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? VideoTrack)?.let { receiveTrack(remoteUid, it) }
            }
        }) ?: run { report("Cannot initialize WebRTC peer"); return }
        pc.addTrack(audioTrack, listOf("group-stream"))
        videoTrack?.let {
            val sender = pc.addTrack(it, listOf("group-stream"))
            val params = sender.parameters
            params.encodings.forEach { encoding ->
                encoding.maxBitrateBps = if (lowData) 100_000 else 380_000
            }
            sender.setParameters(params)
        }
        val pending = CopyOnWriteArrayList<IceCandidate>()
        val listener = pairRef.addSnapshotListener { snap, err ->
            if (err != null) { report("Signaling: ${err.message}"); return@addSnapshotListener }
            val peer = peers[remoteUid] ?: return@addSnapshotListener
            if (snap == null || !snap.exists() || ended) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            val offer = snap.get("offer") as? Map<String, String>
            @Suppress("UNCHECKED_CAST")
            val answer = snap.get("answer") as? Map<String, String>
            if (!initiator && !peer.offerSeen && offer?.get("sdp") != null) {
                peer.offerSeen = true
                setRemote(remoteUid, offer.getValue("sdp"), SessionDescription.Type.OFFER) {
                    createAnswer(remoteUid, pairRef)
                }
            }
            if (initiator && !peer.answerSeen && answer?.get("sdp") != null) {
                peer.answerSeen = true
                setRemote(remoteUid, answer.getValue("sdp"), SessionDescription.Type.ANSWER) {}
            }
        }
        val candidates = pairRef.collection("candidates").addSnapshotListener { snap, err ->
            if (err != null) { report("ICE listener: ${err.message}"); return@addSnapshotListener }
            val peer = peers[remoteUid] ?: return@addSnapshotListener
            snap?.documentChanges?.forEach { change ->
                if (change.type != DocumentChange.Type.ADDED) return@forEach
                val item = change.document
                if (item.getString("sender") == myUid) return@forEach
                val ice = IceCandidate(item.getString("sdpMid").orEmpty(),
                    (item.getLong("sdpMLineIndex") ?: 0L).toInt(), item.getString("candidate").orEmpty())
                if (peer.remoteSdpSet) peer.pc.addIceCandidate(ice) else peer.pending += ice
            }
        }
        peers[remoteUid] = RemotePeer(remoteUid, pc, listener, candidates, pending)
        main.post { onPeerStatus(remoteUid, "Connecting") }
        // Only one deterministic peer creates each pair. Database rules enforce this.
        if (initiator) {
            pairRef.set(mapOf("a" to a, "b" to b,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                .addOnSuccessListener { if (!ended && peers.containsKey(remoteUid)) createOffer(remoteUid, pairRef) }
                .addOnFailureListener { report("Cannot create peer: ${it.message}") }
        }
    }

    private fun receiveTrack(uid: String, track: VideoTrack) {
        main.post {
            val peer = peers[uid] ?: return@post
            if (peer.remoteTrack !== track) {
                peer.renderer?.let { renderer -> peer.remoteTrack?.removeSink(renderer); track.addSink(renderer) }
                peer.remoteTrack = track
            }
        }
    }

    private fun setRemote(uid: String, sdp: String, type: SessionDescription.Type, after: () -> Unit) {
        val peer = peers[uid] ?: return
        peer.pc.setRemoteDescription(object : DescriptionObserver() {
            override fun onSetSuccess() {
                peer.remoteSdpSet = true
                peer.pending.forEach { peer.pc.addIceCandidate(it) }
                peer.pending.clear()
                after()
            }
            override fun onSetFailure(error: String?) { report("Remote SDP: $error") }
        }, SessionDescription(type, sdp))
    }

    private fun createOffer(uid: String, pairRef: com.google.firebase.firestore.DocumentReference) {
        val peer = peers[uid] ?: return
        peer.pc.createOffer(object : DescriptionObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                peer.pc.setLocalDescription(object : DescriptionObserver() {
                    override fun onSetSuccess() {
                        pairRef.update("offer", mapOf("sdp" to desc.description))
                            .addOnFailureListener { report("Offer: ${it.message}") }
                    }
                    override fun onSetFailure(error: String?) { report("Local offer: $error") }
                }, desc)
            }
            override fun onCreateFailure(error: String?) { report("Create offer: $error") }
        }, MediaConstraints())
    }

    private fun createAnswer(uid: String, pairRef: com.google.firebase.firestore.DocumentReference) {
        val peer = peers[uid] ?: return
        peer.pc.createAnswer(object : DescriptionObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                peer.pc.setLocalDescription(object : DescriptionObserver() {
                    override fun onSetSuccess() {
                        pairRef.update("answer", mapOf("sdp" to desc.description))
                            .addOnFailureListener { report("Answer: ${it.message}") }
                    }
                    override fun onSetFailure(error: String?) { report("Local answer: $error") }
                }, desc)
            }
            override fun onCreateFailure(error: String?) { report("Create answer: $error") }
        }, MediaConstraints())
    }

    private fun removePeer(uid: String) {
        val peer = peers.remove(uid) ?: return
        peer.pairListener.remove(); peer.candidateListener.remove()
        peer.renderer?.let { renderer -> peer.remoteTrack?.removeSink(renderer) }
        peer.pc.close(); peer.pc.dispose()
        main.post { onPeerStatus(uid, "Left") }
    }

    fun toggleMic(): Boolean {
        micEnabled = !micEnabled
        audioTrack.setEnabled(micEnabled)
        return micEnabled
    }
    fun toggleCamera(): Boolean {
        if (videoTrack == null) return false
        cameraEnabled = !cameraEnabled
        videoTrack.setEnabled(cameraEnabled)
        return cameraEnabled
    }
    fun switchCamera() { capturer?.switchCamera(null) }
    private fun report(msg: String) { if (!ended) main.post { onError(msg) } }

    fun dispose() {
        if (ended) return
        ended = true
        peers.keys.toList().forEach(::removePeer)
        localRenderer?.let { videoTrack?.removeSink(it) }
        try { capturer?.stopCapture() } catch (_: Exception) {}
        capturer?.dispose(); helper?.dispose()
        videoTrack?.dispose(); audioTrack.dispose()
        videoSource?.dispose(); audioSource.dispose()
        factory.dispose(); egl.release()
        audioManager.mode = originalMode
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = originalSpeaker
    }
}
