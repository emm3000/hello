package com.emm.hello.features.anki

import com.emm.domain.anki.Anki

data class AnkiUiState(
    val anki: Anki? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
