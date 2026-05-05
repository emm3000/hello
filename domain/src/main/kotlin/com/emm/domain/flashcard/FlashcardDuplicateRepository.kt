package com.emm.domain.flashcard

interface FlashcardDuplicateRepository {

    suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean
}
