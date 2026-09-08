package com.emm.data.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.validation.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Test

private const val EXPECTED_TECH_INTERVIEW_NOTE_COUNT = 25
private const val TECH_INTERVIEW_NOTE_ID_PREFIX = "curated.tech-interview."
private val catalogKey: Regex = Regex("^[a-z0-9.-]+$")

class TechInterviewDeckTest {

    private val validate = ValidateGeneratedLearningNoteUseCase()
    private val notes: List<GeneratedLearningNote> = TechInterviewDeck.deck.notes

    @Test
    fun `every note passes validation with no errors and no warnings`() {
        val offenders: List<String> = notes.mapNotNull { note ->
            val result: ValidationResult<GeneratedLearningNote> = validate(note)
            val issues: List<String> = result.issues.map { issue ->
                "${issue.severity}:${issue.code.value}@${issue.field}"
            }
            if (issues.isEmpty()) null else "${note.noteId} -> $issues"
        }

        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `the deck has exactly twenty-five notes`() {
        assertEquals(EXPECTED_TECH_INTERVIEW_NOTE_COUNT, notes.size)
    }

    @Test
    fun `note ids and card ids are unique`() {
        val noteIds: List<String> = notes.map(GeneratedLearningNote::noteId)
        val cardIds: List<String> = notes.flatMap(GeneratedLearningNote::cards)
            .map(GeneratedStudyCard::cardId)

        assertEquals(emptyList<String>(), noteIds.repeatedValues())
        assertEquals(emptyList<String>(), cardIds.repeatedValues())
    }

    @Test
    fun `note ids and card ids are safe catalog keys`() {
        val ids: List<String> = notes.map(GeneratedLearningNote::noteId) +
            notes.flatMap(GeneratedLearningNote::cards).map(GeneratedStudyCard::cardId)
        val offenders: List<String> = ids.filterNot(catalogKey::matches)

        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `every note id carries the deck prefix`() {
        val offenders: List<String> = notes.map(GeneratedLearningNote::noteId)
            .filterNot { it.startsWith(TECH_INTERVIEW_NOTE_ID_PREFIX) }

        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `no two notes share expression, meaning and type`() {
        val keys: List<String> = notes.map { note ->
            "${note.expression.canonical}|${note.intendedMeaningEs.canonical}|${note.noteType}"
        }

        assertEquals(emptyList<String>(), keys.repeatedValues())
    }

    @Test
    fun `every note names its trap`() {
        val offenders: List<String> = notes.filter { note ->
            note.commonMistake.isBlank() || note.confusableWith.isEmpty() || note.sourceContext.isBlank()
        }.map(GeneratedLearningNote::noteId)

        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `every note names the moment in the interview`() {
        val offenders: List<String> = notes
            .filterNot { it.sourceContext.startsWith("Entrevista: ") }
            .map(GeneratedLearningNote::noteId)

        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `word notes carry their lemma and ipa`() {
        val offenders: List<String> = notes
            .filter { it.noteType == LearningNoteType.Word }
            .filter { it.ipa.isBlank() || it.lemma.isBlank() }
            .map(GeneratedLearningNote::noteId)

        assertEquals(emptyList<String>(), offenders)
    }

    private fun List<String>.repeatedValues(): List<String> =
        groupBy { it }.filterValues { it.size > 1 }.keys.sorted()
}
