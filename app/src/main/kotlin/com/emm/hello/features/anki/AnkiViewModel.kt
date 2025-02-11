package com.emm.hello.features.anki

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.anki.AnkiCreator
import kotlinx.coroutines.launch

class AnkiViewModel(private val ankiCreator: AnkiCreator) : ViewModel() {

    fun create(input: String) = viewModelScope.launch {
        try {
            ankiCreator.create(input).also {
                Log.e("aea", it.result)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}