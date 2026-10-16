package com.sayaconnect.app.rtc

import android.content.Context
import android.media.AudioManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sayaconnect.app.BuildConfig
import org.webrtc.*
import java.util.concurrent.CopyOnWriteArrayList

/** One-to-one WebRTC connection; SDP and ICE are signaled using restricted Firestore documents.
 * TURN is critical for carrier-grade NAT / difficult international connections.
 * Media is encrypted via WebRTC DTLS-SRTP; Firestore chat messages are not E2EE.
 */
class RtcSession(
    private val context: Context, private val callId: String,
    private val myUid: String, private val caller: Boolean,
    private val lowData: Boolean, private val onStatus: (String) -> Unit,
    private val onEnded: () -> Unit
) {
    private val callRef = FirebaseFirestore.getInstance().collection("calls").document(callId)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val previousMode = audioManager.mode
    val egl: EglBase = EglBase.create()
    private val factory: PeerConnectionFactory
    private val pc: PeerConnection?
    private val helper: SurfaceTextureHelper?
    private val videoSource: VideoSource?
    private val audioSource: AudioSource?
    private val camera: CameraVideoCapturer?
    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null
    private var remoteTrack: VideoTrack? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var callListener: ListenerRegistration? = null
    private var iceListener: ListenerRegistration? = null
    private val pendingIce = CopyOnWriteArrayList<IceCandidate>()
    private var remoteSdpSet = false
    private var remoteOfferSeen = false
    private var remoteAnswerSeen = false
    private var stopped = false
    var micEnabled = true; private set
    var cameraEnabled = true; private set

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

        val iceServers = mutableListOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        if (BuildConfig.TURN_URL.isNotBlank()) {
            iceServers += PeerConnection.IceServer.builder(BuildConfig.TURN_URL)
                .setUsername(BuildConfig.TURN_USER).setPassword(BuildConfig.TURN_PASSWORD).createIceServer()
        }
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
        }
        pc = factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                onStatus(when (state) {
                    PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> "Connected"
                    PeerConnection.IceConnectionState.DISCONNECTED -> "Reconnecting…"
                    PeerConnection.IceConnectionState.FAILED -> "Connection failed — check TURN server"
                    else -> "Connecting…"
                })
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null && !stopped) {
                    callRef.collection("candidates").add(mapOf(
                        "sender" to myUid, "candidate" to candidate.sdp,
                        "sdpMid" to (candidate.sdpMid ?: ""), "sdpMLineIndex" to candidate.sdpMLineIndex
                    ))
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                stream?.videoTracks?.firstOrNull()?.let { connectRemoteTrack(it) }
            }
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                (receiver?.track() as? VideoTrack)?.let { connectRemoteTrack(it) }
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? VideoTrack)?.let { connectRemoteTrack(it) }
            }
        })
        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("audio", audioSource)
        audioTrack?.let { pc?.addTrack(it, listOf("saya-media")) }

        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(context))
            Camera2Enumerator(context) else Camera1Enumerator(false)
        val name = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
        camera = name?.let { enumerator.createCapturer(it, null) as? CameraVideoCapturer }
        if (camera != null) {
            helper = SurfaceTextureHelper.create("SayaCamera", egl.eglBaseContext)
            videoSource = factory.createVideoSource(false)
            camera.initialize(helper, context.applicationContext, videoSource.capturerObserver)
            camera.startCapture(if (lowData) 320 else 640, if (lowData) 240 else 360, if (lowData) 10 else 20)
            videoTrack = factory.createVideoTrack("video", videoSource)
            videoTrack?.let { pc?.addTrack(it, listOf("saya-media")) }
            pc?.senders?.firstOrNull { it.track()?.kind() == "video" }?.let { sender ->
                val params = sender.parameters
                params.encodings.forEach { it.maxBitrateBps = if (lowData) 150_000 else 700_000 }
                sender.setParameters(params)
            }
        } else {
            helper = null
            videoSource = null
            cameraEnabled = false
        }
    }

    fun attachRemote(view: SurfaceViewRenderer) {
        remoteRenderer = view
        view.init(egl.eglBaseContext, null)
        view.setEnableHardwareScaler(true)
        remoteTrack?.addSink(view)
    }
    fun attachLocal(view: SurfaceViewRenderer) {
        localRenderer = view
        view.init(egl.eglBaseContext, null)
        view.setMirror(true)
        videoTrack?.addSink(view)
    }
    private fun connectRemoteTrack(track: VideoTrack) {
        remoteTrack = track
        remoteRenderer?.let { track.addSink(it) }
    }

    fun start() {
        if (caller) createOffer()
        callListener = callRef.addSnapshotListener { snapshot, _ ->
            if (stopped || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            val status = snapshot.getString("status")
            if (status == "ended" || status == "declined") { onEnded(); return@addSnapshotListener }
            @Suppress("UNCHECKED_CAST")
            val offer = snapshot.get("offer") as? Map<String, String>
            @Suppress("UNCHECKED_CAST")
            val answer = snapshot.get("answer") as? Map<String, String>
            if (!caller && !remoteOfferSeen && offer?.get("sdp") != null) {
                remoteOfferSeen = true
                applyRemote(offer, true)
            }
            if (caller && !remoteAnswerSeen && answer?.get("sdp") != null) {
                remoteAnswerSeen = true
                applyRemote(answer, false)
            }
        }
        iceListener = callRef.collection("candidates").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                if (change.type != com.google.firebase.firestore.DocumentChange.Type.ADDED) return@forEach
                val d = change.document
                if (d.getString("sender") == myUid) return@forEach
                val candidate = IceCandidate(
                    d.getString("sdpMid").orEmpty(), (d.getLong("sdpMLineIndex") ?: 0).toInt(),
                    d.getString("candidate").orEmpty()
                )
                if (remoteSdpSet) pc?.addIceCandidate(candidate) else pendingIce += candidate
            }
        }
    }

    private fun createOffer() {
        pc?.createOffer(object : DescriptionObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) pc.setLocalDescription(object : DescriptionObserver() {
                    override fun onSetSuccess() {
                        callRef.update("offer", mapOf("type" to "offer", "sdp" to desc.description))
                    }
                }, desc)
            }
        }, MediaConstraints())
    }
    private fun applyRemote(data: Map<String, String>, sendAnswer: Boolean) {
        val sdp = data["sdp"] ?: return
        val desc = SessionDescription(
            if (sendAnswer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER, sdp
        )
        pc?.setRemoteDescription(object : DescriptionObserver() {
            override fun onSetSuccess() {
                remoteSdpSet = true
                pendingIce.forEach { pc?.addIceCandidate(it) }
                pendingIce.clear()
                if (sendAnswer) createAnswer()
            }
            override fun onSetFailure(error: String?) { onStatus("SDP error: $error") }
        }, desc)
    }
    private fun createAnswer() {
        pc?.createAnswer(object : DescriptionObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) pc.setLocalDescription(object : DescriptionObserver() {
                    override fun onSetSuccess() {
                        callRef.update("answer", mapOf("type" to "answer", "sdp" to desc.description))
                    }
                }, desc)
            }
        }, MediaConstraints())
    }

    fun toggleMic(): Boolean { micEnabled = !micEnabled; audioTrack?.setEnabled(micEnabled); return micEnabled }
    fun toggleCamera(): Boolean { cameraEnabled = !cameraEnabled; videoTrack?.setEnabled(cameraEnabled); return cameraEnabled }
    fun switchCamera() { camera?.switchCamera(null) }
    fun dispose() {
        if (stopped) return
        stopped = true
        callListener?.remove(); iceListener?.remove()
        remoteRenderer?.let { remoteTrack?.removeSink(it); it.release() }
        localRenderer?.let { videoTrack?.removeSink(it); it.release() }
        try { camera?.stopCapture() } catch (_: Exception) {}
        camera?.dispose(); helper?.dispose()
        pc?.close(); pc?.dispose()
        videoTrack?.dispose(); audioTrack?.dispose()
        videoSource?.dispose(); audioSource?.dispose()
        factory.dispose(); egl.release()
        audioManager.mode = previousMode
    }
}

open class DescriptionObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
