package com.jp.whatsappclone.notifications

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
import com.google.firebase.auth.FirebaseAuth
import com.jp.whatsappclone.MainActivity
import com.jp.whatsappclone.R
import com.jp.whatsappclone.data.repository.DeviceTokenRegistrar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InnovfixMessagingService : FirebaseMessagingService() {
    @Inject lateinit var tokenRegistrar: DeviceTokenRegistrar

    override fun onNewToken(token: String) {
        tokenRegistrar.registerInBackground(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        if (message.data[KEY_ORG_ID] != "innovfix") return
        val currentUserId = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() ?: return
        if (message.data[KEY_RECIPIENT_ID] != currentUserId) return
        val conversationId = message.data[KEY_CONVERSATION_ID] ?: return
        val messageId = message.data[KEY_MESSAGE_ID] ?: conversationId
        if (!recordIfNew(messageId)) return
        val title = message.notification?.title ?: message.data[KEY_TITLE] ?: getString(R.string.app_name)
        val preview = message.notification?.body ?: message.data[KEY_PREVIEW] ?: "New message"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Internal message notifications"
                },
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(KEY_CONVERSATION_ID, conversationId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(conversationId, NOTIFICATION_ID, notification)
    }

    @Synchronized
    private fun recordIfNew(messageId: String): Boolean {
        val preferences = getSharedPreferences(DEDUP_PREFERENCES, android.content.Context.MODE_PRIVATE)
        if (preferences.contains(messageId)) return false
        val stored = preferences.all.entries
            .sortedByDescending { (_, value) -> value as? Long ?: 0L }
        val editor = preferences.edit().putLong(messageId, System.currentTimeMillis())
        stored.drop(MAX_DEDUP_IDS - 1).forEach { editor.remove(it.key) }
        editor.apply()
        return true
    }

    companion object {
        const val KEY_CONVERSATION_ID = "conversationId"
        const val KEY_MESSAGE_ID = "messageId"
        private const val KEY_ORG_ID = "orgId"
        private const val KEY_RECIPIENT_ID = "recipientId"
        private const val KEY_TITLE = "title"
        private const val KEY_PREVIEW = "preview"
        private const val CHANNEL_ID = "messages"
        private const val DEDUP_PREFERENCES = "message-notification-dedup"
        private const val MAX_DEDUP_IDS = 500
        private const val NOTIFICATION_ID = 1

        fun createNotificationChannel(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Internal message notifications"
                    },
                )
            }
        }

        fun clearForSignedOutUser(context: android.content.Context) {
            context.getSystemService(NotificationManager::class.java).cancelAll()
            context.getSharedPreferences(DEDUP_PREFERENCES, android.content.Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}
