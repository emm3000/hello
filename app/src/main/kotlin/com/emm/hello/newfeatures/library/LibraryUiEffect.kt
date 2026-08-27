package com.emm.hello.newfeatures.library

import com.emm.hello.core.mvi.MviEffect

sealed interface LibraryUiEffect : MviEffect {
    data class OpenCard(val cardId: String, val deckId: String) : LibraryUiEffect
    data object OpenCapture : LibraryUiEffect
}
