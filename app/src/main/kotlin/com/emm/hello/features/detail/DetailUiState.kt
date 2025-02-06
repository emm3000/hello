package com.emm.hello.features.detail

import com.emm.domain.Word
import com.emm.domain.WordContent

data class DetailUiState(
    val currentWord: Word? = null,
    val contentWord: WordContent? = null,
    val isLoading: Boolean = false,
)