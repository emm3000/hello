package com.emm.data.deck

import com.emm.data.HelloDb
import com.emm.data.SyncStatus
import com.emm.data.flashcard.CreateExampleRequest
import com.emm.data.flashcard.CreateFlashcardRequest
import com.emm.data.flashcard.CreateFlashcardReviewRequest

class RequestDataProcessor(helloDb: HelloDb) {

    private val dq = helloDb.deckQueries
    private val fq = helloDb.flashcardQueries
    private val eq = helloDb.flashcardExampleQueries
    private val fr = helloDb.flashcardReviewQueries

    fun process(data: Any) {
        if (data is CreateDeckRequest) {
            dq.insert(
                id = data.id,
                name = data.name,
                description = data.description,
                createdAt = data.createdAt,
                updatedAt = data.updatedAt,
                syncStatus = SyncStatus.Synced.name
            )
        }

        if (data is CreateFlashcardRequest) {
            fq.create(
                id = data.id,
                deckId = data.deckId,
                word = data.word,
                meaning = data.meaning,
                translation = data.translation,
                phonetic = data.phonetic,
                createdAt = data.createdAt,
                updatedAt = data.updatedAt,
                syncStatus = SyncStatus.Synced.name
            )
        }

        if (data is CreateExampleRequest) {
            eq.insert(
                id = data.id,
                flashcardId = data.flashcardId,
                text = data.text,
                translation = data.translation,
                type = data.type,
                createdAt = data.createdAt,
                updatedAt = data.updatedAt,
                syncStatus = SyncStatus.Synced.name,
            )
        }

        if (data is CreateFlashcardReviewRequest) {
            fr.upsertFlashcardReview(
                flashcardId = data.flashcardId,
                lastReviewedAt = data.lastReviewedAt,
                nextReviewAt = data.nextReviewAt,
                easeFactor = data.easeFactor,
                interval = data.interval,
                repetitions = data.repetitions,
                lapses = data.lapses,
                createdAt = data.createdAt,
                updatedAt = data.updatedAt,
                syncStatus = SyncStatus.Synced.name,
            )
        }
    }
}