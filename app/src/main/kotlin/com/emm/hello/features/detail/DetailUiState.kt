package com.emm.hello.features.detail

import com.emm.domain.Word
import com.emm.domain.WordContent

data class DetailUiState(
    val currentWord: Word? = null,
    val scrapContentWord: WordContent? = null,
    val iaContentWord: WordContent? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDeleteSuccess: Boolean = false,
)