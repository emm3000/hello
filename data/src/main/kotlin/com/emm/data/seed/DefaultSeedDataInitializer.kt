package com.emm.data.seed

import com.emm.data.remote.DataStore
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.DeckRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.seed.SeedDataInitializer
import kotlinx.coroutines.flow.first
import java.util.UUID

// Guard order, flag-gated in every branch: flag set means done; flag unset but decks exist
// means an existing user who must not be seeded, only flagged; flag unset and no decks is the
// only path that seeds. The deck name arrives from the app layer so this module needs no
// Android resource lookup.
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
            val flashcardId: FlashcardId = UUID.randomUUID().toString().toFlashcardId()
            flashcardRepository.create(
                CreateFlashcardInput(
                    id = flashcardId,
                    deckId = deckId,
                    word = card.word,
                    meaning = card.meaning,
                    translation = card.translation,
                    phonetic = card.phonetic,
                    partOfSpeech = card.partOfSpeech,
                ),
            )
            flashcardRepository.upsertExamples(
                examples = listOf(
                    Example(
                        exampleId = flashcardId.value,
                        text = card.example,
                        translation = card.exampleTranslation,
                        type = "main",
                    ),
                ),
                flashcardId = flashcardId,
            )
        }
    }

    private companion object {
        // The app targets Spanish speakers learning English, so this content is not localized.
        val STARTER_CARDS: List<StarterCard> = listOf(
            StarterCard(
                "hello",
                "hola",
                "A friendly greeting used when you meet someone.",
                "/həˈloʊ/",
                "interjection",
                "She waved and said hello to everyone.",
                "Saludó con la mano y dijo hola a todos.",
            ),
            StarterCard(
                "water",
                "agua",
                "The clear liquid that fills rivers and falls as rain.",
                "/ˈwɔːtər/",
                "noun",
                "I drink a glass of water every morning.",
                "Bebo un vaso de agua cada mañana.",
            ),
            StarterCard(
                "friend",
                "amigo/a",
                "Someone you know well and like, outside your family.",
                "/frɛnd/",
                "noun",
                "My best friend lives next door.",
                "Mi mejor amigo vive al lado.",
            ),
            StarterCard(
                "to eat",
                "comer",
                "To put food in your mouth and swallow it.",
                "/iːt/",
                "verb",
                "We like to eat dinner together.",
                "Nos gusta comer juntos.",
            ),
            StarterCard(
                "house",
                "casa",
                "A building where people live.",
                "/haʊs/",
                "noun",
                "Their house has a red door.",
                "Su casa tiene una puerta roja.",
            ),
            StarterCard(
                "book",
                "libro",
                "A set of printed pages bound together to read.",
                "/bʊk/",
                "noun",
                "I finished the book last night.",
                "Terminé el libro anoche.",
            ),
            StarterCard(
                "to work",
                "trabajar",
                "To do a job, usually to earn money.",
                "/wɜːrk/",
                "verb",
                "He wants to work in another country.",
                "Él quiere trabajar en otro país.",
            ),
            StarterCard(
                "thank you",
                "gracias",
                "A phrase you say to show gratitude.",
                "/ˈθæŋk juː/",
                "phrase",
                "Thank you for coming to my party.",
                "Gracias por venir a mi fiesta.",
            ),
        )
    }
}

private data class StarterCard(
    val word: String,
    val translation: String,
    val meaning: String,
    val phonetic: String,
    val partOfSpeech: String,
    val example: String,
    val exampleTranslation: String,
)
