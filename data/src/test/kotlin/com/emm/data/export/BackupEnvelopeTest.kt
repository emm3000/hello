package com.emm.data.export

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEnvelopeTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun `DeckDto serializes and deserializes correctly`() {
        val dto = DeckDto(
            id = "deck-1",
            name = "Spanish",
            description = "Vocabulary",
            createdAt = 100L,
            updatedAt = 200L,
            deletedAt = null,
        )

        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<DeckDto>(serialized)

        assertEquals(dto.id, deserialized.id)
        assertEquals(dto.name, deserialized.name)
        assertEquals(dto.description, deserialized.description)
        assertEquals(dto.createdAt, deserialized.createdAt)
        assertEquals(dto.deletedAt, deserialized.deletedAt)
    }

    @Test
    fun `DeckDto with deletedAt serializes correctly`() {
        val dto = DeckDto(
            id = "deck-deleted",
            name = "Deleted",
            description = null,
            createdAt = 100L,
            updatedAt = 200L,
            deletedAt = 300L,
        )

        val serialized = json.encodeToString(dto)
        assertTrue(serialized.contains("\"deletedAt\": 300"))
    }

    @Test
    fun `FlashcardDto serializes and deserializes correctly`() {
        val dto = FlashcardDto(
            id = "card-1",
            deckId = "deck-1",
            word = "hola",
            meaning = "hello",
            translation = "hola",
            phonetic = "/oʊla/",
            partOfSpeech = "interjection",
            type = null,
            note = null,
            register = null,
            levelBand = null,
            domain = null,
            lemma = null,
            whyUseful = null,
            usagePattern = null,
            irregularFormsJson = null,
            collocationsJson = null,
            commonMistake = null,
            confusableWithJson = null,
            clozeSentence = null,
            sourceContext = null,
            warningsJson = null,
            studyCardsJson = null,
            qualityChecksJson = null,
            createdAt = 100L,
            updatedAt = 200L,
            deletedAt = null,
        )

        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<FlashcardDto>(serialized)

        assertEquals("card-1", deserialized.id)
        assertEquals("deck-1", deserialized.deckId)
        assertEquals("hola", deserialized.word)
        assertEquals("hello", deserialized.meaning)
    }

    @Test
    fun `BackupEnvelope round-trip preserves all fields`() {
        val envelope = BackupEnvelope(
            schemaVersion = 1,
            exportedAt = 1234567890L,
            decks = listOf(
                DeckDto("d1", "Deck 1", "desc", 100L, 200L, null),
                DeckDto("d2", "Deck 2", null, 100L, 200L, null),
            ),
            flashcards = listOf(
                FlashcardDto(
                    id = "c1",
                    deckId = "d1",
                    word = "word",
                    meaning = "meaning",
                    translation = null,
                    phonetic = null,
                    partOfSpeech = null,
                    type = null,
                    note = null,
                    register = null,
                    levelBand = null,
                    domain = null,
                    lemma = null,
                    whyUseful = null,
                    usagePattern = null,
                    irregularFormsJson = null,
                    collocationsJson = null,
                    commonMistake = null,
                    confusableWithJson = null,
                    clozeSentence = null,
                    sourceContext = null,
                    warningsJson = null,
                    studyCardsJson = null,
                    qualityChecksJson = null,
                    createdAt = 100L,
                    updatedAt = 200L,
                    deletedAt = null,
                ),
            ),
            examples = listOf(
                FlashcardExampleDto("e1", "c1", "text", "translation", "example", 100L, 200L, null),
            ),
            tags = listOf(
                TagDto("t1", "spanish", 100L, null),
            ),
            deckTags = listOf(
                DeckTagDto("t1", "d1", 300L),
            ),
            reviewEvents = listOf(
                ReviewEventDto("ev1", "c1", "GOOD", 1000L, 2000L, 2.5, 1L, 1L, 0L, 1000L),
            ),
            reviewProjections = listOf(
                ReviewProjectionDto("c1", 1000L, 2000L, 2.5, 1L, 1L, 0L, "ev1", 2000L),
            ),
        )

        val serialized = json.encodeToString(envelope)
        val deserialized = json.decodeFromString<BackupEnvelope>(serialized)

        assertEquals(1, deserialized.schemaVersion)
        assertEquals(2, deserialized.decks.size)
        assertEquals(1, deserialized.flashcards.size)
        assertEquals(1, deserialized.examples.size)
        assertEquals(1, deserialized.tags.size)
        assertEquals(1, deserialized.deckTags.size)
        assertEquals(1, deserialized.reviewEvents.size)
        assertEquals(1, deserialized.reviewProjections.size)

        assertEquals("Deck 1", deserialized.decks[0].name)
        assertEquals("spanish", deserialized.tags[0].name)
    }

    @Test
    fun `BackupEnvelope with empty lists serializes correctly`() {
        val envelope = BackupEnvelope(
            schemaVersion = 1,
            exportedAt = 0L,
            decks = emptyList(),
            flashcards = emptyList(),
            examples = emptyList(),
            tags = emptyList(),
            deckTags = emptyList(),
            reviewEvents = emptyList(),
            reviewProjections = emptyList(),
        )

        val serialized = json.encodeToString(envelope)
        assertTrue(serialized.contains("\"decks\": []"))
        assertTrue(serialized.contains("\"flashcards\": []"))
    }

    @Test
    fun `TagDto serializes and deserializes correctly`() {
        val dto = TagDto(
            id = "tag-1",
            name = "travel",
            createdAt = 100L,
            deletedAt = null,
        )

        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<TagDto>(serialized)

        assertEquals("tag-1", deserialized.id)
        assertEquals("travel", deserialized.name)
    }

    @Test
    fun `DeckTagDto serializes and deserializes correctly`() {
        val dto = DeckTagDto(
            tagId = "tag-1",
            deckId = "deck-1",
            createdAt = 300L,
        )

        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<DeckTagDto>(serialized)

        assertEquals("tag-1", deserialized.tagId)
        assertEquals("deck-1", deserialized.deckId)
        assertEquals(300L, deserialized.createdAt)
    }

    @Test
    fun `ReviewEventDto serializes and deserializes correctly`() {
        val dto = ReviewEventDto(
            eventId = "ev-1",
            flashcardId = "c1",
            grade = "GOOD",
            reviewedAt = 1000L,
            nextReviewAt = 2000L,
            easeFactor = 2.5,
            interval = 1L,
            repetitions = 1L,
            lapses = 0L,
            createdAt = 1000L,
        )

        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<ReviewEventDto>(serialized)

        assertEquals("ev-1", deserialized.eventId)
        assertEquals("GOOD", deserialized.grade)
        assertEquals(2.5, deserialized.easeFactor, 0.001)
    }

    @Test
    fun `ReviewProjectionDto serializes and deserializes correctly`() {
        val dto = ReviewProjectionDto(
            flashcardId = "c1",
            lastReviewedAt = 1000L,
            nextReviewAt = 2000L,
            easeFactor = 2.5,
            interval = 1L,
            repetitions = 1L,
            lapses = 0L,
            sourceEventId = "ev-1",
            updatedAt = 2000L,
        )

        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<ReviewProjectionDto>(serialized)

        assertEquals("c1", deserialized.flashcardId)
        assertEquals(2000L, deserialized.nextReviewAt)
        assertEquals(2.5, deserialized.easeFactor, 0.001)
    }
}
