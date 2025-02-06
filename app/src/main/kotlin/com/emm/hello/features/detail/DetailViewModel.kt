package com.emm.hello.features.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.Word
import com.emm.domain.WordContent
import com.emm.domain.WordContentCreator
import com.emm.domain.WordContentFetcher
import com.emm.domain.WordRepository
import kotlinx.coroutines.launch

class DetailViewModel(
    private val wordContentCreator: WordContentCreator,
    private val wordContentFetcher: WordContentFetcher,
    private val wordRepository: WordRepository,
) : ViewModel() {

    var state by mutableStateOf(DetailUiState())
        private set

    fun detail(wordId: String) = viewModelScope.launch {
        val wordDetail: Word = wordRepository.selectBy(wordId) ?: return@launch
        val wordContent: WordContent? = wordContentFetcher.fetch(wordId = wordId)
        state = state.copy(
            currentWord = wordDetail,
            hasContent = wordContent != null,
            contentWord = wordContent,
        )
    }

    fun contentCreator(word: Word) = viewModelScope.launch {
        state = state.copy(isLoading = true)
        wordContentCreator.create(word)
        val wordContent: WordContent = wordContentFetcher.fetch(wordId = word.id) ?: return@launch
        state = state.copy(contentWord = wordContent, isLoading = false, hasContent = true)
    }
}