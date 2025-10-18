package com.emm.hello.legacy.detail

import com.emm.domain.deprecated.word.Word
import com.emm.domain.deprecated.word.WordContent

data class DetailUiState(
    val currentWord: Word? = null,
    val scrapContentWord: WordContent? = null,
    val iaContentWord: WordContent? = null,
    val ankiContentWord: WordContent? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDeleteSuccess: Boolean = false,
)