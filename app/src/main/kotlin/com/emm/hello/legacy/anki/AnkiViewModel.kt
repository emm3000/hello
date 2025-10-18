package com.emm.hello.legacy.anki

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deprecated.anki.AnkiCreator
import com.emm.domain.deprecated.word.WordContent
import kotlinx.coroutines.launch

class AnkiViewModel(private val ankiCreator: AnkiCreator) : ViewModel() {

    var state by mutableStateOf(AnkiUiState())
        private set

    fun create(input: String) = viewModelScope.launch {
        try {
            state = state.copy(isLoading = true)
            val result: WordContent = ankiCreator.create(input)
            state = state.copy(anki = result, isLoading = false)
        } catch (e: Throwable) {
            state = state.copy(isLoading = false, errorMessage = e.message.orEmpty())
            e.printStackTrace()
        }
    }
}