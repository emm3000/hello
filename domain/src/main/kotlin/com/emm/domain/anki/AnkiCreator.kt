package com.emm.domain.anki

import com.emm.domain.word.Word
import com.emm.domain.word.WordContent
import com.emm.domain.word.WordRepository
import java.time.Instant
import java.util.UUID

class AnkiCreator(
    private val wordRepository: WordRepository,
    private val repository: AnkiRepository,
) {

    suspend fun create(input: String): WordContent {
        val word = Word(
            id = UUID.randomUUID().toString(),
            word = input,
            hasContent = false,
            createdAt = Instant.now().toEpochMilli(),
        )
        wordRepository.upsert(word)
        return repository.createCard(word)
    }
}