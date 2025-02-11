package com.emm.hello.features.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.word.SourceType
import com.emm.domain.word.Word
import com.emm.domain.word.WordContentCreator
import com.emm.domain.word.WordContentFetcher
import com.emm.domain.word.WordRepository
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
        val wordContent: WordContentFetcher.HolderOfWordContent = wordContentFetcher.fetch(wordId = wordId)
        state = state.copy(
            currentWord = wordDetail,
            iaContentWord = wordContent.iaContent,
            scrapContentWord = wordContent.scrapContent,
            ankiContentWord = wordContent.iaAnkiContent,
        )
    }

    fun contentCreator(word: Word, sourceType: SourceType) = viewModelScope.launch {
        try {
            state = state.copy(isLoading = true)
            wordContentCreator.create(word, sourceType)
            val wordContent: WordContentFetcher.HolderOfWordContent = wordContentFetcher.fetch(wordId = word.id)
            val wordDetail: Word = wordRepository.selectBy(wordId = word.id) ?: return@launch
            state = state.copy(
                isLoading = false,
                scrapContentWord = wordContent.scrapContent,
                iaContentWord = wordContent.iaContent,
                currentWord = wordDetail,
            )
        } catch (e: Exception) {
            state = state.copy(
                isLoading = true,
                errorMessage = e.message.orEmpty(),
            )
        }
    }

    fun delete(wordId: String) = viewModelScope.launch {
        wordRepository.deleteBy(wordId)
        state = state.copy(isDeleteSuccess = true)
    }
}