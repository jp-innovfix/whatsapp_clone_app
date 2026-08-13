package com.jp.whatsappclone.data.repository

import android.content.Context
import android.app.NotificationManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import com.jp.whatsappclone.data.remote.FirebaseServices
import com.jp.whatsappclone.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class DeviceTokenRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val services: FirebaseServices,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    val installationId: String
        get() = preferences.getString(INSTALLATION_ID, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(INSTALLATION_ID, it).apply()
        }

    fun registerInBackground(token: String? = null) {
        applicationScope.launch {
            runCatching {
                if (token == null) registerCurrentToken() else register(token)
            }
        }
    }

    suspend fun registerCurrentToken() {
        services.app ?: return
        val token = FirebaseMessaging.getInstance().token.await()
        register(token)
    }

    suspend fun register(token: String) {
        if (token.isBlank()) return
        val userId = services.auth?.currentUser?.uid ?: return
        val firestore = services.firestore ?: return
        val reference = firestore.collection("members")
            .document(userId)
            .collection("devices")
            .document(installationId)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(reference)
            if (existing.exists()) {
                transaction.update(
                    reference,
                    mapOf(
                        "token" to token,
                        "platform" to "ANDROID",
                        "enabled" to true,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            } else {
                transaction.set(
                    reference,
                    mapOf(
                        "installationId" to installationId,
                        "token" to token,
                        "platform" to "ANDROID",
                        "enabled" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
        }.await()
    }

    suspend fun disableCurrentInstallation(userId: String? = services.auth?.currentUser?.uid) {
        userId ?: return
        val firestore = services.firestore ?: return
        runCatching {
            firestore.collection("members")
                .document(userId)
                .collection("devices")
                .document(installationId)
                .update(mapOf("enabled" to false, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
        }
    }

    fun disableCurrentInstallationInBackground(userId: String?) {
        applicationScope.launch { runCatching { disableCurrentInstallation(userId) } }
    }

    fun clearLocalNotificationState() {
        com.jp.whatsappclone.notifications.InnovfixMessagingService.clearForSignedOutUser(context)
    }

    private companion object {
        const val PREFERENCES = "innovfix-device"
        const val INSTALLATION_ID = "installation-id"
    }
}
