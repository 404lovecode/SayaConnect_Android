package com.sayaconnect.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.sayaconnect.app.MainActivity
import com.sayaconnect.app.R

class CallPushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("pushTokens").document(userId)
            .set(mapOf("token" to token))
    }
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["kind"] !in listOf("call", "group_call")) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(NotificationChannel("incoming_calls", "Incoming calls", NotificationManager.IMPORTANCE_HIGH))
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("callId", message.data["callId"])
        }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notice = NotificationCompat.Builder(this, "incoming_calls")
                .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(if (message.data["kind"] == "group_call")
                "SayaConnect · Group call invitation" else "SayaConnect · Incoming video call")
            .setContentText(message.data["fromName"] ?: "A friend is calling")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true).setContentIntent(pi).build()
        mgr.notify(1102, notice)
    }
}
