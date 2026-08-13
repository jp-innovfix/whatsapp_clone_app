package com.jp.whatsappclone

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

internal object AppCheckProviderSelector {
    fun factory(@Suppress("UNUSED_PARAMETER") useEmulator: Boolean): AppCheckProviderFactory =
        PlayIntegrityAppCheckProviderFactory.getInstance()
}
