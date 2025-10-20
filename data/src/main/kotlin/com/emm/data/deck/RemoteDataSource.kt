package com.emm.data.deck

import com.emm.data.flashcard.CreateExampleRequest
import com.emm.data.flashcard.CreateFlashcardRequest
import com.emm.data.flashcard.CreateFlashcardReviewRequest
import com.emm.data.remote.ApiService

class RemoteDataSource(private val apiService: ApiService) {

    suspend fun createDeck(decks: List<CreateDeckRequest>) {
        apiService.createDecks(decks)
    }

    suspend fun createFlashcard(flashcards: List<CreateFlashcardRequest>) {
        apiService.createFlashcard(flashcards)
    }

    suspend fun createExample(example: List<CreateExampleRequest>) {
        apiService.createExamples(example)
    }

    suspend fun createReview(newReviews: List<CreateFlashcardReviewRequest>) {
        apiService.createReviews(newReviews)
    }
}
