package com.jp.whatsappclone

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.jp.whatsappclone.data.repository.PresenceManager
import com.jp.whatsappclone.notifications.InnovfixMessagingService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WhatsAppCloneApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var presenceManager: PresenceManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.FIREBASE_CONFIGURED) return

        val firebaseApp = FirebaseApp.getApps(this).firstOrNull()
            ?: if (BuildConfig.FIREBASE_EMULATOR_ENABLED) {
                FirebaseApp.initializeApp(
                    this,
                    FirebaseOptions.Builder()
                        .setProjectId(EMULATOR_PROJECT_ID)
                        .setApplicationId(EMULATOR_APPLICATION_ID)
                        .setApiKey(EMULATOR_API_KEY)
                        .build(),
                )
            } else {
                FirebaseApp.initializeApp(this)
            }
            ?: return
        FirebaseAppCheck.getInstance(firebaseApp).installAppCheckProviderFactory(
            AppCheckProviderSelector.factory(BuildConfig.FIREBASE_EMULATOR_ENABLED),
        )
        presenceManager.install()
        InnovfixMessagingService.createNotificationChannel(this)
    }

    private companion object {
        const val EMULATOR_PROJECT_ID = "demo-innovfix-internal"
        const val EMULATOR_APPLICATION_ID = "1:1234567890:android:innovfixinternal"
        const val EMULATOR_API_KEY = "emulator-api-key"
    }
}
