package com.emm.hello.features.anki

import com.emm.domain.word.WordContent

data class AnkiUiState(
    val anki: WordContent? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
