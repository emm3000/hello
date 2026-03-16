package com.emm.hello.newfeatures.deck

sealed interface DeckDetailUiIntent {
    data object Refresh : DeckDetailUiIntent
}
