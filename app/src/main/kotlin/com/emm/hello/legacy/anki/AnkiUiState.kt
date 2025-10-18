package com.emm.hello.legacy.anki

import com.emm.domain.deprecated.word.WordContent

data class AnkiUiState(
    val anki: WordContent? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
