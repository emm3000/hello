package com.emm.data.flashcard

import com.emm.data.flashcard.iadto.GeneratedLearningNoteDto
import com.emm.data.flashcard.iadto.GeneratedLearningNoteResponseDto
import com.emm.data.flashcard.iadto.GeneratedNoteQualityCheckDto
import com.emm.data.flashcard.iadto.GeneratedStudyCardDto
import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedNoteQualityCode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag
import com.emm.domain.generation.RegisterPreference
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.validation.DomainValidationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object GeneratedLearningNoteResponseParser {

    private val validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase()

    fun parse(raw: String, json: Json): GeneratedLearningNote {
        return try {
            val response = json.decodeFromString<GeneratedLearningNoteResponseDto>(raw)
            val data = response.data
            if (!response.success || data == null) {
                val message = response.error?.message ?: "Unknown AI error"
                throw IllegalArgumentException(message)
            }
            data.toValidatedDomain()
        } catch (error: SerializationException) {
            throw IllegalArgumentException(
                "La respuesta de la IA no coincide con el formato esperado para learning note",
                error,
            )
        }
    }

    private fun GeneratedLearningNoteDto.toValidatedDomain(): GeneratedLearningNote {
        val note = GeneratedLearningNote.fromSemanticCore(
            noteId = noteId,
            noteType = noteType.toLearningNoteType(),
            expression = expression.toExpression(),
            intendedMeaningEs = intendedMeaningEs.toIntendedMeaningEs(),
            simpleDefinitionEn = simpleDefinitionEn.toDefinitionEn(),
            partOfSpeech = partOfSpeech.toPartOfSpeechTag(),
            register = register.toRegisterPreference(),
            levelBand = levelBand.toLevelBand(),
            domain = domain.toLearningDomain(),
            whyUseful = whyUseful,
            exampleSentence = exampleSentence,
            exampleTranslation = exampleTranslation,
            cards = cards.map { it.toDomain() },
            qualityChecks = qualityChecks.map { it.toDomain() },
            lemma = lemma,
            ipa = ipa,
            usagePattern = usagePattern,
            irregularForms = irregularForms,
            collocations = collocations,
            commonMistake = commonMistake,
            confusableWith = confusableWith,
            clozeSentence = clozeSentence,
            sourceContext = sourceContext,
            warnings = warnings,
        )
        val validation = validateGeneratedLearningNoteUseCase(note)
        if (!validation.isValid) {
            throw DomainValidationException(validation.errors)
        }
        return note
    }

    private fun GeneratedStudyCardDto.toDomain(): GeneratedStudyCard {
        return GeneratedStudyCard(
            cardId = cardId,
            cardType = cardType.toStudyCardType(),
            prompt = prompt,
            expectedAnswer = expectedAnswer,
            evaluationMode = evaluationMode.toEvaluationMode(),
            isActive = isActive,
            acceptedAnswers = acceptedAnswers,
            hint = hint,
            explanation = explanation,
            sourceField = sourceField,
        )
    }

    private fun GeneratedNoteQualityCheckDto.toDomain(): GeneratedNoteQualityCheck {
        return GeneratedNoteQualityCheck(
            code = code.toGeneratedNoteQualityCode(),
            passed = passed,
            message = message,
        )
    }

    private fun String.toLearningNoteType(): LearningNoteType {
        return when (normalizedEnum()) {
            "word" -> LearningNoteType.Word
            "phrase" -> LearningNoteType.Phrase
            "phrasal_verb" -> LearningNoteType.PhrasalVerb
            "idiom" -> LearningNoteType.Idiom
            "sentence_pattern" -> LearningNoteType.SentencePattern
            "chunk" -> LearningNoteType.Phrase // AI sometimes confuses chunk (part_of_speech) with note_type
            else -> throw invalidEnum("LearningNoteType")
        }
    }

    private fun String.toStudyCardType(): StudyCardType {
        return when (normalizedEnum()) {
            "recognition" -> StudyCardType.Recognition
            "production" -> StudyCardType.Production
            "cloze" -> StudyCardType.Cloze
            "form" -> StudyCardType.Form
            else -> throw invalidEnum("StudyCardType")
        }
    }

    private fun String.toEvaluationMode(): EvaluationMode {
        return when (normalizedEnum()) {
            "exact" -> EvaluationMode.Exact
            "flexible_text" -> EvaluationMode.FlexibleText
            "manual_self_check" -> EvaluationMode.ManualSelfCheck
            else -> throw invalidEnum("EvaluationMode")
        }
    }

    private fun String.toPartOfSpeechTag(): PartOfSpeechTag {
        return when (normalizedEnum()) {
            "noun" -> PartOfSpeechTag.Noun
            "verb" -> PartOfSpeechTag.Verb
            "adjective" -> PartOfSpeechTag.Adjective
            "adverb" -> PartOfSpeechTag.Adverb
            "preposition" -> PartOfSpeechTag.Preposition
            "conjunction" -> PartOfSpeechTag.Conjunction
            "interjection" -> PartOfSpeechTag.Interjection
            "phrasal_verb" -> PartOfSpeechTag.PhrasalVerb
            "idiom" -> PartOfSpeechTag.Idiom
            "chunk" -> PartOfSpeechTag.Chunk
            "other" -> PartOfSpeechTag.Other
            else -> throw invalidEnum("PartOfSpeechTag")
        }
    }

    private fun String.toRegisterPreference(): RegisterPreference {
        return when (normalizedEnum()) {
            "casual" -> RegisterPreference.Casual
            "neutral" -> RegisterPreference.Neutral
            "formal" -> RegisterPreference.Formal
            else -> throw invalidEnum("RegisterPreference")
        }
    }

    private fun String.toLevelBand(): LevelBand {
        return when (normalizedEnum()) {
            "a1_a2" -> LevelBand.A1_A2
            "b1_b2" -> LevelBand.B1_B2
            "c1_plus" -> LevelBand.C1_PLUS
            else -> throw invalidEnum("LevelBand")
        }
    }

    private fun String.toLearningDomain(): LearningDomain {
        return when (normalizedEnum()) {
            "daily_life" -> LearningDomain.DailyLife
            "travel" -> LearningDomain.Travel
            "social" -> LearningDomain.Social
            "work" -> LearningDomain.Work
            "study" -> LearningDomain.Study
            "media" -> LearningDomain.Media
            "mixed" -> LearningDomain.Mixed
            else -> throw invalidEnum("LearningDomain")
        }
    }

    private fun String.toGeneratedNoteQualityCode(): GeneratedNoteQualityCode {
        return when (normalizedEnum()) {
            "single_meaning" -> GeneratedNoteQualityCode.SingleMeaning
            "natural_example" -> GeneratedNoteQualityCode.NaturalExample
            "example_supports_meaning" -> GeneratedNoteQualityCode.ExampleSupportsMeaning
            "non_ambiguous_answers" -> GeneratedNoteQualityCode.NonAmbiguousAnswers
            "required_fields_present" -> GeneratedNoteQualityCode.RequiredFieldsPresent
            "clear_card_focus" -> GeneratedNoteQualityCode.ClearCardFocus
            "note_card_alignment" -> GeneratedNoteQualityCode.NoteCardAlignment
            else -> throw invalidEnum("GeneratedNoteQualityCode")
        }
    }

    private fun String.normalizedEnum(): String {
        return trim()
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .lowercase()
    }

    private fun String.invalidEnum(typeName: String): IllegalArgumentException {
        return IllegalArgumentException("Unknown $typeName value: $this")
    }
}
