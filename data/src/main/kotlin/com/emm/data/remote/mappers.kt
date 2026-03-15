package com.emm.data.remote

import com.emm.data.Deck
import com.emm.data.Flashcard
import com.emm.data.FlashcardExample
import com.emm.data.FlashcardReview
import com.emm.data.Quote
import java.security.MessageDigest
import java.time.Instant

fun String.toSha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun deckToDto(deck: Deck) = DeckUpsertRequest(
    deckId = deck.id,
    name = deck.name,
    description = deck.description.orEmpty(),
    createdAt = deck.createdAt.toString(),
    updatedAt = deck.updatedAt.toString(),
)

fun flashcardToDto(flashcard: Flashcard) = CardUpsertRequest(
    id = flashcard.id,
    word = flashcard.word,
    meaning = flashcard.meaning,
    translation = flashcard.translation.orEmpty(),
    phonetic = flashcard.phonetic.orEmpty(),
    note = flashcard.note.orEmpty(),
    createdAt = flashcard.createdAt.toString(),
    updatedAt = flashcard.updatedAt.toString(),
    deckId = flashcard.deckId,
)

fun exampleToDto(example: FlashcardExample) = ExampleUpsertRequest(
    id = example.id,
    text = example.text,
    translation = example.translation,
    type = example.type,
    flashcardId = example.flashcardId.orEmpty(),
    createdAt = example.createdAt.toString(),
    updatedAt = example.updatedAt.toString(),
)

fun quoteToDto(quote: Quote) = QuoteUpsertRequest(
    id = quote.id,
    title = quote.title,
    phrase = quote.phrase,
    description = quote.description,
    translation = quote.translation,
    example = quote.example,
    context = quote.context,
    pronunciation = quote.pronunciation,
    formality = quote.formality,
    tags = quote.tags,
    createdAt = quote.createdAt.toString(),
    updatedAt = quote.updatedAt.toString(),
)

fun reviewToDto(review: FlashcardReview) = FlashcardReviewUpsertRequest(
    flashcardId = review.flashcardId,
    lastReviewedAt = review.lastReviewedAt ?: Instant.now().toEpochMilli(),
    nextReviewAt = review.nextReviewAt ?: Instant.now().toEpochMilli(),
    easeFactor = review.easeFactor,
    interval = review.interval,
    repetitions = review.repetitions,
    lapses = review.lapses,
    createdAt = review.createdAt.toString(),
    updatedAt = review.updatedAt.toString(),
)
