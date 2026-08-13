package com.jp.whatsappclone.data

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class AppViewModel(
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val repository = MockBusinessRepository()
    val appState = repository.appState

    fun dispatch(action: AppAction) = repository.dispatch(action)
}
