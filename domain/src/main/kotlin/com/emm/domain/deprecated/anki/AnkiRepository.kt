package com.emm.domain.deprecated.anki

import com.emm.domain.deprecated.word.Word
import com.emm.domain.deprecated.word.WordContent

interface AnkiRepository {

    suspend fun createCard(word: Word): WordContent
}