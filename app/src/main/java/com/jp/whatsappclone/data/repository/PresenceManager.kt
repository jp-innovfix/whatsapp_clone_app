package com.jp.whatsappclone.data.repository

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.jp.whatsappclone.data.remote.BackendConfiguration
import com.jp.whatsappclone.data.remote.FirebaseServices
import com.jp.whatsappclone.data.domain.SessionState
import com.jp.whatsappclone.di.ApplicationScope
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

@Singleton
class PresenceManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val services: FirebaseServices,
    private val configuration: BackendConfiguration,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : DefaultLifecycleObserver {
    private var heartbeatJob: Job? = null
    @Volatile private var processIsForeground = false

    fun install() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        applicationScope.launch {
            authRepository.observeSession()
                .map { session -> (session as? SessionState.SignedIn)?.user?.id }
                .distinctUntilChanged()
                .filterNotNull()
                .collectLatest {
                if (processIsForeground) runCatching { writePresence(online = true) }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        processIsForeground = true
        heartbeatJob?.cancel()
        heartbeatJob = applicationScope.launch {
            while (isActive) {
                runCatching { writePresence(online = true) }
                delay(HEARTBEAT_MILLIS)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        processIsForeground = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        applicationScope.launch { runCatching { writePresence(online = false) } }
    }

    private suspend fun writePresence(online: Boolean) {
        val userId = services.auth?.currentUser?.uid ?: return
        val firestore = services.firestore ?: return
        val update = mutableMapOf<String, Any?>(
            "lastSeenAt" to FieldValue.serverTimestamp(),
            "activeUntil" to if (online) Timestamp(Date(System.currentTimeMillis() + ONLINE_WINDOW_MILLIS)) else null,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("organizations")
            .document(configuration.organizationId)
            .collection("members")
            .document(userId)
            .update(update)
            .await()
    }

    private companion object {
        const val HEARTBEAT_MILLIS = 60_000L
        const val ONLINE_WINDOW_MILLIS = 90_000L
    }
}
