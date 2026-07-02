package com.emm.data.seed

import com.emm.data.remote.DataStore
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.DeckRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.ids.toDeckId
import com.emm.domain.seed.SeedDataInitializer
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Creates a small starter deck the first time an install opens with no decks.
 *
 * Guard order (one-time check, flag-gated in every branch):
 * 1. Flag already set -> return, do nothing.
 * 2. Flag not set but the user already has decks -> existing user, do NOT seed; just set the flag.
 * 3. Flag not set and no decks -> new install, seed the deck + cards, then set the flag.
 *
 * The deck name is provided by the app layer (resolved from string resources) so this module stays
 * free of Android resource lookups. Cards are inserted through [FlashcardRepository.create], the
 * same path the UI uses, so they land in FSRS NEW state via the existing ReviewProjection defaults.
 */
class DefaultSeedDataInitializer(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val dataStore: DataStore,
    private val deckName: String,
) : SeedDataInitializer {

    override suspend fun ensureSeeded() {
        if (dataStore.hasSeededStarterDeck) return

        val hasExistingDecks = deckRepository.fetchAll().first().isNotEmpty()
        if (!hasExistingDecks) {
            seedStarterDeck()
        }
        dataStore.hasSeededStarterDeck = true
    }

    private suspend fun seedStarterDeck() {
        val deckId = UUID.randomUUID().toString().toDeckId()
        deckRepository.create(
            CreateDeckInput(
                name = deckName,
                description = "",
                id = deckId,
            ),
        )
        STARTER_CARDS.forEach { card ->
            flashcardRepository.create(
                CreateFlashcardInput(
                    deckId = deckId,
                    word = card.word,
                    meaning = card.meaning,
                    translation = card.translation,
                    phonetic = card.phonetic,
                    partOfSpeech = card.partOfSpeech,
                ),
            )
        }
    }

    private companion object {
        // Front (English) with an English gloss, IPA and part of speech; back is Spanish.
        // The app targets Spanish speakers learning English, so this content is not localized.
        val STARTER_CARDS: List<StarterCard> = listOf(
            StarterCard("hello", "hola", "A friendly greeting used when you meet someone.", "/həˈloʊ/", "interjection"),
            StarterCard("water", "agua", "The clear liquid that fills rivers and seas and falls as rain.", "/ˈwɔːtər/", "noun"),
            StarterCard("friend", "amigo/a", "Someone you know well and like, outside your family.", "/frɛnd/", "noun"),
            StarterCard("to eat", "comer", "To put food in your mouth and swallow it.", "/iːt/", "verb"),
            StarterCard("house", "casa", "A building where people live.", "/haʊs/", "noun"),
            StarterCard("book", "libro", "A set of printed pages bound together to read.", "/bʊk/", "noun"),
            StarterCard("to work", "trabajar", "To do a job, usually to earn money.", "/wɜːrk/", "verb"),
            StarterCard("thank you", "gracias", "A phrase you say to show gratitude.", "/ˈθæŋk juː/", "phrase"),
        )
    }
}

private data class StarterCard(
    val word: String,
    val translation: String,
    val meaning: String,
    val phonetic: String,
    val partOfSpeech: String,
)
