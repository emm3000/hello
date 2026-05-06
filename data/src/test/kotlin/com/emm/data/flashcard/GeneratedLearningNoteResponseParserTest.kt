package com.emm.data.flashcard

import com.emm.domain.generation.GeneratedNoteQualityCode
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag
import com.emm.domain.generation.RegisterPreference
import com.emm.domain.generation.StudyCardType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeneratedLearningNoteResponseParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val validLearningNoteRaw = """
        {
          "success": true,
          "data": {
            "note_id": "note-1",
            "note_type": "phrasal_verb",
            "expression": "pick up",
            "intended_meaning_es": "recoger",
            "simple_definition_en": "to go somewhere and get someone or something",
            "part_of_speech": "phrasal_verb",
            "register": "neutral",
            "level_band": "A1_A2",
            "domain": "daily_life",
            "why_useful": "Sirve para hablar de tareas y movimientos cotidianos.",
            "example_sentence": "I'll pick you up after work.",
            "example_translation": "Te recojo despues del trabajo.",
            "usage_pattern": "pick someone up",
            "cloze_sentence": "I'll ____ you up after work.",
            "cards": [
              {
                "card_id": "card-1",
                "card_type": "recognition",
                "prompt": "pick up",
                "expected_answer": "recoger",
                "evaluation_mode": "flexible_text",
                "is_active": true
              },
              {
                "card_id": "card-2",
                "card_type": "production",
                "prompt": "Como dices 'recoger' en ingles?",
                "expected_answer": "pick up",
                "evaluation_mode": "exact",
                "is_active": true
              },
              {
                "card_id": "card-3",
                "card_type": "cloze",
                "prompt": "I'll ____ you up after work.",
                "expected_answer": "pick",
                "evaluation_mode": "exact",
                "is_active": true
              }
            ],
            "quality_checks": [
              {
                "code": "single_meaning",
                "passed": true,
                "message": "ok"
              },
              {
                "code": "natural_example",
                "passed": true,
                "message": "ok"
              },
              {
                "code": "example_supports_meaning",
                "passed": true,
                "message": "ok"
              },
              {
                "code": "non_ambiguous_answers",
                "passed": true,
                "message": "ok"
              },
              {
                "code": "required_fields_present",
                "passed": true,
                "message": "ok"
              },
              {
                "code": "clear_card_focus",
                "passed": true,
                "message": "ok"
              },
              {
                "code": "note_card_alignment",
                "passed": true,
                "message": "ok"
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `parse returns generated learning note`() {
        val result = GeneratedLearningNoteResponseParser.parse(validLearningNoteRaw, json)

        assertEquals("note-1", result.noteId)
        assertEquals(LearningNoteType.PhrasalVerb, result.noteType)
        assertEquals("pick up", result.expression)
        assertEquals(PartOfSpeechTag.PhrasalVerb, result.partOfSpeech)
        assertEquals(RegisterPreference.Neutral, result.register)
        assertEquals(LevelBand.A1_A2, result.levelBand)
        assertEquals(LearningDomain.DailyLife, result.domain)
        assertEquals(3, result.cards.size)
        assertEquals(StudyCardType.Cloze, result.cards[2].cardType)
        assertEquals(GeneratedNoteQualityCode.SingleMeaning, result.qualityChecks.first().code)
    }

    @Test
    fun `parse with unknown enum throws illegal argument exception`() {
        val raw = """
            {
              "success": true,
              "data": {
                "note_id": "note-1",
                "note_type": "unknown_type",
                "expression": "pick up",
                "intended_meaning_es": "recoger",
                "simple_definition_en": "to get something",
                "part_of_speech": "verb",
                "register": "neutral",
                "level_band": "A1_A2",
                "domain": "daily_life",
                "why_useful": "Sirve para situaciones cotidianas.",
                "example_sentence": "I pick up my son at school.",
                "example_translation": "Recojo a mi hijo en el colegio.",
                "cards": [],
                "quality_checks": []
              }
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            GeneratedLearningNoteResponseParser.parse(raw, json)
        }
    }

    @Test
    fun `parse with failed quality check throws illegal argument exception`() {
        val raw = """
            {
              "success": true,
              "data": {
                "note_id": "note-1",
                "note_type": "word",
                "expression": "borrow",
                "intended_meaning_es": "pedir prestado",
                "simple_definition_en": "to take something and return it later",
                "part_of_speech": "verb",
                "register": "neutral",
                "level_band": "A1_A2",
                "domain": "daily_life",
                "why_useful": "Sirve para hablar de prestamos.",
                "example_sentence": "Can I borrow your pen?",
                "example_translation": "Puedo pedirte prestado tu lapicero?",
                "cards": [
                  {
                    "card_id": "card-1",
                    "card_type": "recognition",
                    "prompt": "borrow",
                    "expected_answer": "pedir prestado",
                    "evaluation_mode": "flexible_text"
                  },
                  {
                    "card_id": "card-2",
                    "card_type": "production",
                    "prompt": "Como dices pedir prestado en ingles?",
                    "expected_answer": "borrow",
                    "evaluation_mode": "exact"
                  }
                ],
                "quality_checks": [
                  {
                    "code": "single_meaning",
                    "passed": true,
                    "message": "ok"
                  },
                  {
                    "code": "non_ambiguous_answers",
                    "passed": false,
                    "message": "La respuesta sigue siendo ambigua."
                  }
                ]
              }
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            GeneratedLearningNoteResponseParser.parse(raw, json)
        }
    }

    @Test
    fun `parse without cards field throws illegal argument exception`() {
        val raw = """
            {
              "success": true,
              "data": {
                "note_id": "note-1",
                "note_type": "word",
                "expression": "borrow",
                "intended_meaning_es": "pedir prestado",
                "simple_definition_en": "to take something and return it later",
                "part_of_speech": "verb",
                "register": "neutral",
                "level_band": "A1_A2",
                "domain": "daily_life",
                "why_useful": "Sirve para hablar de prestamos.",
                "example_sentence": "Can I borrow your pen?",
                "example_translation": "Puedo pedirte prestado tu lapicero?",
                "quality_checks": [
                  {
                    "code": "single_meaning",
                    "passed": true,
                    "message": "ok"
                  }
                ]
              }
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            GeneratedLearningNoteResponseParser.parse(raw, json)
        }
    }
}
