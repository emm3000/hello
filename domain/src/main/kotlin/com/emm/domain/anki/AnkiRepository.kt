package com.emm.domain.anki

import com.emm.domain.word.Word
import com.emm.domain.word.WordContent

interface AnkiRepository {

    suspend fun createCard(word: Word): WordContent
}