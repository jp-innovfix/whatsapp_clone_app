package com.jp.whatsappclone.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.jp.whatsappclone.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface BackendConfiguration {
    val firebaseConfigured: Boolean
    val organizationId: String
    val functionsRegion: String
}

@Singleton
class AppBackendConfiguration @Inject constructor() : BackendConfiguration {
    override val firebaseConfigured: Boolean = BuildConfig.FIREBASE_CONFIGURED
    override val organizationId: String = BuildConfig.ORGANIZATION_ID
    override val functionsRegion: String = BuildConfig.FIREBASE_FUNCTIONS_REGION
}

/** Lazily obtains Firebase SDKs so local previews/tests never require a default FirebaseApp. */
@Singleton
class FirebaseServices @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configuration: BackendConfiguration,
) {
    val app: FirebaseApp? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        if (!configuration.firebaseConfigured) null
        else FirebaseApp.getApps(context).firstOrNull()
            ?: if (BuildConfig.FIREBASE_EMULATOR_ENABLED) {
                FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                        .setProjectId(EMULATOR_PROJECT_ID)
                        .setApplicationId(EMULATOR_APPLICATION_ID)
                        .setApiKey(EMULATOR_API_KEY)
                        .build(),
                )
            } else {
                FirebaseApp.initializeApp(context)
            }
    }

    val auth: FirebaseAuth? by lazy {
        app?.let(FirebaseAuth::getInstance)?.also { instance ->
            if (BuildConfig.FIREBASE_EMULATOR_ENABLED) instance.useEmulator(EMULATOR_HOST, 9099)
        }
    }
    val firestore: FirebaseFirestore? by lazy {
        app?.let(FirebaseFirestore::getInstance)?.also { instance ->
            instance.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            if (BuildConfig.FIREBASE_EMULATOR_ENABLED) instance.useEmulator(EMULATOR_HOST, 8080)
        }
    }
    val functions: FirebaseFunctions? by lazy {
        app?.let { FirebaseFunctions.getInstance(it, configuration.functionsRegion) }?.also { instance ->
            if (BuildConfig.FIREBASE_EMULATOR_ENABLED) instance.useEmulator(EMULATOR_HOST, 5001)
        }
    }
    val storage: FirebaseStorage? by lazy {
        app?.let(FirebaseStorage::getInstance)?.also { instance ->
            if (BuildConfig.FIREBASE_EMULATOR_ENABLED) instance.useEmulator(EMULATOR_HOST, 9199)
        }
    }

    val authState: Flow<String?>
        get() = callbackFlow {
            val authInstance = auth
            if (authInstance == null) {
                trySend(null)
                close()
                return@callbackFlow
            }
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
            authInstance.addAuthStateListener(listener)
            awaitClose { authInstance.removeAuthStateListener(listener) }
        }

    private companion object {
        // Android emulator loopback alias. Physical-device testing should use production or
        // temporarily replace this with the development machine's LAN address.
        const val EMULATOR_HOST = "10.0.2.2"
        const val EMULATOR_PROJECT_ID = "demo-innovfix-internal"
        const val EMULATOR_APPLICATION_ID = "1:1234567890:android:innovfixinternal"
        const val EMULATOR_API_KEY = "emulator-api-key"
    }
}
