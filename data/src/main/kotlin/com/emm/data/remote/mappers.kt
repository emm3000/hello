package com.emm.data.remote

import com.emm.data.Deck
import com.emm.data.Flashcard
import com.emm.data.FlashcardExample
import com.emm.data.Quote
import java.security.MessageDigest

fun String.toSha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun deckToDto(deck: Deck) = DeckDto(
    deckId = deck.id,
    name = deck.name,
    description = deck.description.orEmpty(),
    createdAt = deck.createdAt
)

fun flashcardToDto(flashcard: Flashcard) = CardDto(
    id = flashcard.id,
    word = flashcard.word,
    meaning = flashcard.meaning,
    translation = flashcard.translation.orEmpty(),
    phonetic = flashcard.phonetic.orEmpty(),
    audioPath = flashcard.audioPath.orEmpty(),
    imagePath = flashcard.imagePath.orEmpty(),
    note = flashcard.note.orEmpty(),
    createdAt = flashcard.createdAt,
    isGenerated = flashcard.isGenerated.toInt(),
    deckId = flashcard.deckId,
)

fun exampleToDto(example: FlashcardExample) = ExampleDto(
    id = example.id,
    text = example.text,
    translation = example.translation,
    type = example.type,
    flashcardId = example.flashcardId.orEmpty()
)

fun quoteToDto(quote: Quote) = QuoteDto(
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
    createdAt = quote.createdAt
)