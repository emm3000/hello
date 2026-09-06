package com.emm.domain.generation

import com.emm.domain.authoring.sampleWordNote
import com.emm.domain.flashcard.toExpression
import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratedLearningNoteExamplePolicyTest {

    private val policy = GeneratedLearningNoteExamplePolicy()

    @Test
    fun `collectIssues returns no error when the example contains the expression`() {
        val issues = policy.collectIssues(sampleWordNote())

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `collectIssues matches case insensitively`() {
        val note = sampleWordNote().copy(
            exampleSentence = "BORROW my notes if you want.",
        )

        val issues = policy.collectIssues(note)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `collectIssues matches an inflected form of a single word expression`() {
        val note = sampleWordNote().copy(
            expression = "walk".toExpression(),
            lemma = "",
            exampleSentence = "She walked home.",
        )

        val issues = policy.collectIssues(note)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `collectIssues matches through the lemma`() {
        val note = sampleWordNote().copy(
            expression = "ran".toExpression(),
            lemma = "run",
            exampleSentence = "I run every morning.",
        )

        val issues = policy.collectIssues(note)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `collectIssues matches through an irregular form of a phrasal verb`() {
        val note = sampleWordNote().copy(
            expression = "give up".toExpression(),
            lemma = "give up",
            irregularForms = listOf("gave up", "given up"),
            exampleSentence = "He gave it up last year.",
        )

        val issues = policy.collectIssues(note)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `collectIssues returns example does not use expression error when no form appears`() {
        val note = sampleWordNote().copy(
            expression = "borrow".toExpression(),
            exampleSentence = "Can I have your pen?",
        )

        val issues = policy.collectIssues(note)

        assertEquals(1, issues.size)
        assertEquals(IssueCode.ExampleDoesNotUseExpression, issues.first().code)
        assertEquals("exampleSentence", issues.first().field)
    }

    @Test
    fun `collectIssues ignores a blank example`() {
        val note = sampleWordNote().copy(
            exampleSentence = "   ",
        )

        val issues = policy.collectIssues(note)

        assertTrue(issues.isEmpty())
    }
}
