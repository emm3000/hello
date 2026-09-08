package com.emm.hello.newfeatures.store

import com.emm.hello.core.mvi.MviIntent

sealed interface StoreUiIntent : MviIntent {
    data object BackRequested : StoreUiIntent
    data class InstallRequested(val curatedDeckId: String) : StoreUiIntent
    data class OpenDeckRequested(val deckId: String) : StoreUiIntent
}
