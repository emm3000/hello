package com.emm.data.deck

import com.emm.data.flashcard.CreateExampleRequest
import com.emm.data.flashcard.CreateFlashcardRequest
import com.emm.data.flashcard.CreateFlashcardReviewRequest
import kotlinx.serialization.KSerializer

enum class TypeFile(val fileNameFromRemote: String, val serializer: KSerializer<*>) {

    Decks("decks.json", CreateDeckRequest.serializer()),
    Flashcards("flashcards.json", CreateFlashcardRequest.serializer()),
    Examples("examples.json", CreateExampleRequest.serializer()),
    Reviews("reviews.json", CreateFlashcardReviewRequest.serializer());

    companion object {

        fun fromFileName(fileName: String): TypeFile? {
            return entries.find { it.fileNameFromRemote == fileName }
        }
    }
}