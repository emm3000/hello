@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.emm.hello.legacy.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.data.deprecated.word.WordDao
import com.emm.data.deprecated.word.WordEntity
import com.emm.domain.deprecated.word.Word
import com.emm.domain.deprecated.word.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val wordDao: WordDao,
    private val wordRepository: WordRepository,
) : ViewModel() {

    var searchState by mutableStateOf("")
        private set

    val words: StateFlow<List<WordEntity>> = snapshotFlow { searchState }
        .debounce(250L)
        .distinctUntilChanged()
        .flatMapLatest(wordDao::searchBy)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun updateSearch(value: String) {
        searchState = value
    }

    fun createWord(word: String) = viewModelScope.launch {
        val newWord = Word.create(
            word = word.trim(),
            hasContent = false,
        )
        wordRepository.upsert(newWord)
    }
}