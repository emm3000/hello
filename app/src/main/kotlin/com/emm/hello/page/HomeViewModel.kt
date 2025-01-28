package com.emm.hello.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.data.WordDao
import com.emm.data.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class HomeViewModel(private val wordDao: WordDao) : ViewModel() {

    val wordListFlow: StateFlow<List<WordEntity>> = wordDao.all()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun insert(word: String) = viewModelScope.launch {
        val wordEntity = WordEntity(
            id = UUID.randomUUID().toString(),
            word = word,
        )
        withContext(Dispatchers.IO) {
            wordDao.insert(wordEntity)
        }
    }
}