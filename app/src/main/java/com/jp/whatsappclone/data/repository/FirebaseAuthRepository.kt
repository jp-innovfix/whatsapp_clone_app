package com.jp.whatsappclone.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctionsException
import com.jp.whatsappclone.data.domain.SessionState
import com.jp.whatsappclone.data.local.InnovfixDatabase
import com.jp.whatsappclone.data.mapper.toDomain
import com.jp.whatsappclone.data.mapper.toMemberEntity
import com.jp.whatsappclone.data.remote.BackendConfiguration
import com.jp.whatsappclone.data.remote.FirebaseServices
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val services: FirebaseServices,
    private val configuration: BackendConfiguration,
    private val database: InnovfixDatabase,
    private val tokenRegistrar: DeviceTokenRegistrar,
    private val outboxScheduler: com.jp.whatsappclone.data.sync.OutboxScheduler,
    @com.jp.whatsappclone.di.ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope,
) : AuthRepository {
    private val sessionMutationMutex = Mutex()

    private val sharedSession: Flow<SessionState> = createSessionFlow()
        .shareIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            replay = 1,
        )

    override fun observeSession(): Flow<SessionState> = sharedSession

    private fun createSessionFlow(): Flow<SessionState> = callbackFlow {
        val auth = services.auth
        val firestore = services.firestore
        if (auth == null || firestore == null) {
            launch { database.clearUserData() }
            trySend(SessionState.SignedOut)
            close()
            return@callbackFlow
        }

        var generation = 0L
        var tokenRegisteredUserId: String? = null
        var memberListener: ListenerRegistration? = null
        var memberRetryJob: Job? = null

        fun isCurrent(requestGeneration: Long, userId: String): Boolean =
            requestGeneration == generation && auth.currentUser?.uid == userId

        fun publishSignedIn(requestGeneration: Long, userId: String, member: com.jp.whatsappclone.data.local.MemberEntity) {
            if (!isCurrent(requestGeneration, userId)) return
            trySend(SessionState.SignedIn(member.toDomain()))
            if (tokenRegisteredUserId != userId) {
                tokenRegisteredUserId = userId
                outboxScheduler.schedule()
                launch {
                    runCatching { tokenRegistrar.registerCurrentToken() }
                        .onFailure {
                            if (isCurrent(requestGeneration, userId) && tokenRegisteredUserId == userId) {
                                // A later validated member snapshot retries registration.
                                tokenRegisteredUserId = null
                            }
                        }
                }
            }
        }

        suspend fun publishCachedSessionIfEligible(requestGeneration: Long, userId: String) {
            val cachedMember = sessionMutationMutex.withLock {
                if (!isCurrent(requestGeneration, userId)) null
                else database.cachedActiveSelfProfile(userId, configuration.organizationId)
            }
            if (cachedMember != null) publishSignedIn(requestGeneration, userId, cachedMember)
        }

        suspend fun invalidateConfirmedSession(requestGeneration: Long, userId: String, reason: String) {
            sessionMutationMutex.withLock {
                if (!isCurrent(requestGeneration, userId)) return@withLock
                database.clearUserData()
                tokenRegistrar.clearLocalNotificationState()
                tokenRegisteredUserId = null
                trySend(SessionState.Disabled(reason))
                auth.signOut()
            }
        }

        fun attachMemberListener(requestGeneration: Long, userId: String) {
            if (!isCurrent(requestGeneration, userId)) return
            memberListener?.remove()
            memberListener = firestore.collection("organizations")
                .document(configuration.organizationId)
                .collection("members")
                .document(userId)
                .addSnapshotListener { snapshot, failure ->
                    if (!isCurrent(requestGeneration, userId)) return@addSnapshotListener
                    launch {
                        when {
                            failure?.isTransientMemberFailure() == true -> {
                                // A transient outage may use only this exact owner's previously
                                // server-validated active profile. Keep retrying the authoritative
                                // listener so a later disable/delete is enforced immediately.
                                publishCachedSessionIfEligible(requestGeneration, userId)
                                memberRetryJob?.cancel()
                                memberRetryJob = launch {
                                    delay(MEMBER_LISTENER_RETRY_MILLIS)
                                    if (isCurrent(requestGeneration, userId)) {
                                        attachMemberListener(requestGeneration, userId)
                                    }
                                }
                            }

                            failure != null && auth.currentUser?.isAnonymous == true &&
                                failure.isAnonymousBootstrapPending() -> {
                                trySend(SessionState.Loading)
                                memberRetryJob?.cancel()
                                memberRetryJob = launch {
                                    delay(ANONYMOUS_BOOTSTRAP_RETRY_MILLIS)
                                    if (isCurrent(requestGeneration, userId)) {
                                        attachMemberListener(requestGeneration, userId)
                                    }
                                }
                            }

                            failure != null -> invalidateConfirmedSession(
                                requestGeneration = requestGeneration,
                                userId = userId,
                                reason = "Unable to verify employee access",
                            )

                            snapshot?.metadata?.isFromCache == true -> {
                                // A cache-only Firestore event is not evidence that a missing
                                // member was deleted. Room's validated owner/profile pair is the
                                // sole offline credential.
                                publishCachedSessionIfEligible(requestGeneration, userId)
                            }

                            else -> {
                                val member = snapshot?.toMemberEntity(configuration.organizationId)
                                when {
                                    member == null && auth.currentUser?.isAnonymous == true -> {
                                        trySend(SessionState.Loading)
                                        memberRetryJob?.cancel()
                                        memberRetryJob = launch {
                                            delay(ANONYMOUS_BOOTSTRAP_RETRY_MILLIS)
                                            if (isCurrent(requestGeneration, userId)) {
                                                attachMemberListener(requestGeneration, userId)
                                            }
                                        }
                                    }

                                    member == null -> invalidateConfirmedSession(
                                        requestGeneration,
                                        userId,
                                        "Employee profile is unavailable",
                                    )

                                    member.memberId != userId || member.orgId != configuration.organizationId ->
                                        invalidateConfirmedSession(
                                            requestGeneration,
                                            userId,
                                            "Employee profile does not match this account",
                                        )

                                    !member.active -> invalidateConfirmedSession(
                                        requestGeneration,
                                        userId,
                                        "Employee access is disabled",
                                    )

                                    else -> {
                                        val stored = sessionMutationMutex.withLock {
                                            if (!isCurrent(requestGeneration, userId)) false
                                            else database.storeValidatedSelfProfile(
                                                ownerUid = userId,
                                                orgId = configuration.organizationId,
                                                member = member,
                                            )
                                        }
                                        if (stored) publishSignedIn(requestGeneration, userId, member)
                                    }
                                }
                            }
                        }
                    }
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { currentAuth ->
            memberListener?.remove()
            memberListener = null
            memberRetryJob?.cancel()
            memberRetryJob = null
            val user = currentAuth.currentUser
            val requestGeneration = ++generation
            launch {
                if (user == null) {
                    sessionMutationMutex.withLock {
                        if (requestGeneration != generation || auth.currentUser != null) return@withLock
                        database.clearUserData()
                        tokenRegistrar.clearLocalNotificationState()
                        tokenRegisteredUserId = null
                        trySend(SessionState.SignedOut)
                    }
                    return@launch
                }

                val prepared = sessionMutationMutex.withLock {
                    if (!isCurrent(requestGeneration, user.uid)) false
                    else {
                        database.prepareForOwner(user.uid, configuration.organizationId)
                        true
                    }
                }
                if (!prepared || !isCurrent(requestGeneration, user.uid)) return@launch
                trySend(SessionState.Loading)
                attachMemberListener(requestGeneration, user.uid)
            }
        }
        auth.addAuthStateListener(authListener)
        awaitClose {
            memberRetryJob?.cancel()
            memberListener?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    override suspend fun ensureAnonymousSession(): Result<Unit> = runCatching {
        val auth = services.auth ?: error("Firebase is not configured for this build")
        if (auth.currentUser?.isAnonymous == false) {
            sessionMutationMutex.withLock {
                database.clearUserData()
                tokenRegistrar.clearLocalNotificationState()
                auth.signOut()
            }
        }
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Firebase did not return an anonymous user")
        check(user.isAnonymous) { "Anonymous Firebase sign-in is required" }

        val functions = services.functions ?: error("Firebase Functions is not configured for this build")
        functions.getHttpsCallable(ANONYMOUS_BOOTSTRAP_CALLABLE).call().await()
        user.getIdToken(true).await()
        Unit
    }.recoverCatching { failure ->
        if (failure is FirebaseFunctionsException) {
            error(failure.message ?: "Unable to create the anonymous INNOVFIX profile")
        }
        throw failure
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        require(email.isNotBlank()) { "Email is required" }
        require(password.isNotBlank()) { "Password is required" }
        val auth = services.auth ?: error("Firebase is not configured for this build")
        val firestore = services.firestore ?: error("Firebase is not configured for this build")
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Firebase did not return a user")
        sessionMutationMutex.withLock {
            check(auth.currentUser?.uid == user.uid) { "Authentication changed while signing in" }
            database.prepareForOwner(user.uid, configuration.organizationId)
        }
        val member = firestore.collection("organizations")
            .document(configuration.organizationId)
            .collection("members")
            .document(user.uid)
            .get(Source.SERVER)
            .await()
            .toMemberEntity(configuration.organizationId)
        if (
            member == null ||
            !member.active ||
            member.memberId != user.uid ||
            member.orgId != configuration.organizationId
        ) {
            sessionMutationMutex.withLock {
                if (auth.currentUser?.uid == user.uid) {
                    database.clearUserData()
                    tokenRegistrar.clearLocalNotificationState()
                    auth.signOut()
                }
            }
            error("This employee account is not active")
        }
        val stored = sessionMutationMutex.withLock {
            auth.currentUser?.uid == user.uid && database.storeValidatedSelfProfile(
                ownerUid = user.uid,
                orgId = configuration.organizationId,
                member = member,
            )
        }
        check(stored) { "Authentication changed while verifying employee access" }
        tokenRegistrar.registerCurrentToken()
    }

    override suspend fun signOut() {
        // The local security boundary comes first; remote token cleanup is bounded and best effort.
        val userId = services.auth?.currentUser?.uid
        sessionMutationMutex.withLock {
            database.clearUserData()
            tokenRegistrar.clearLocalNotificationState()
        }
        withTimeoutOrNull(1_500) { tokenRegistrar.disableCurrentInstallation(userId) }
        sessionMutationMutex.withLock {
            // A member snapshot could have raced the bounded remote cleanup; wipe once more
            // immediately before revoking the local Firebase session.
            database.clearUserData()
            services.auth?.signOut()
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit> = runCatching {
        val userId = services.auth?.currentUser?.uid ?: error("Sign in is required")
        val firestore = services.firestore ?: error("Firebase is not configured for this build")
        firestore.collection("organizations")
            .document(configuration.organizationId)
            .collection("members")
            .document(userId)
            .update(
                mapOf(
                    "notificationPreference" to if (enabled) "ALL" else "NONE",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    private companion object {
        const val MEMBER_LISTENER_RETRY_MILLIS = 5_000L
        const val ANONYMOUS_BOOTSTRAP_RETRY_MILLIS = 1_000L
        const val ANONYMOUS_BOOTSTRAP_CALLABLE = "bootstrapAnonymousMember"
    }
}

private fun Throwable.isTransientMemberFailure(): Boolean {
    val code = (this as? FirebaseFirestoreException)?.code ?: return false
    return code in setOf(
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.CANCELLED,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.INTERNAL,
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.UNKNOWN,
    )
}

private fun Throwable.isAnonymousBootstrapPending(): Boolean {
    val code = (this as? FirebaseFirestoreException)?.code ?: return false
    return code == FirebaseFirestoreException.Code.PERMISSION_DENIED || isTransientMemberFailure()
}
