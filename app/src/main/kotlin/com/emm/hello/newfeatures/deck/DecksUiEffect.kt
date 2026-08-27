package com.emm.hello.newfeatures.deck

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface DecksUiEffect : MviEffect {
    data class OpenDeckForm(val deckId: String?) : DecksUiEffect
    data class ShowUndoDeckDeleted(
        val deckName: String,
        val deckId: String,
        val deletedAt: Long,
    ) : DecksUiEffect

    data class ShowMessage(@StringRes val messageRes: Int) : DecksUiEffect
}
