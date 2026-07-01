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
        STARTER_CARDS.forEach { (english, spanish) ->
            flashcardRepository.create(
                CreateFlashcardInput(
                    deckId = deckId,
                    word = english,
                    meaning = "",
                    translation = spanish,
                    phonetic = "",
                ),
            )
        }
    }

    private companion object {
        // Front (English) to back (Spanish). Fixed learning content: the app targets Spanish
        // speakers learning English, so these pairs are not localized.
        val STARTER_CARDS: List<Pair<String, String>> = listOf(
            "hello" to "hola",
            "water" to "agua",
            "friend" to "amigo/a",
            "to eat" to "comer",
            "house" to "casa",
            "book" to "libro",
            "to work" to "trabajar",
            "thank you" to "gracias",
        )
    }
}
