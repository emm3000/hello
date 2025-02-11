package com.emm.hello.features.detail

import com.emm.domain.word.Word
import com.emm.domain.word.WordContent

data class DetailUiState(
    val currentWord: Word? = null,
    val scrapContentWord: WordContent? = null,
    val iaContentWord: WordContent? = null,
    val ankiContentWord: WordContent? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDeleteSuccess: Boolean = false,
)