package com.emm.hello.newfeatures.study

import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.StudyCardType
import com.emm.domain.study.ReviewGrade
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StudyAnswerPolicyTest {

    @Test
    fun `exact matching ignores casing punctuation and extra spacing`() {
        val matches = matchesTypedAnswer(
            evaluationMode = EvaluationMode.Exact,
            typedAnswer = "  Casualidad, afortunada! ",
            expectedAnswer = "casualidad afortunada",
            acceptedAnswers = emptyList(),
        )

        assertThat(matches).isTrue()
    }

    @Test
    fun `flexible text accepts contained variants`() {
        val matches = matchesTypedAnswer(
            evaluationMode = EvaluationMode.FlexibleText,
            typedAnswer = "to give up on it completely",
            expectedAnswer = "give up",
            acceptedAnswers = emptyList(),
        )

        assertThat(matches).isTrue()
    }

    @Test
    fun `manual self check never auto matches`() {
        val matches = matchesTypedAnswer(
            evaluationMode = EvaluationMode.ManualSelfCheck,
            typedAnswer = "anything",
            expectedAnswer = "anything",
            acceptedAnswers = listOf("anything else"),
        )

        assertThat(matches).isFalse()
    }

    @Test
    fun `correct typed answer blocks again and keeps higher grades`() {
        val policy = productionCard().gradePolicy(
            typedAnswerChecked = true,
            typedAnswerCorrect = true,
        )

        assertThat(policy.enabledGrades).containsExactly(
            ReviewGrade.HARD,
            ReviewGrade.GOOD,
            ReviewGrade.EASY,
        )
    }

    @Test
    fun `incorrect typed answer allows only again and hard`() {
        val policy = productionCard().gradePolicy(
            typedAnswerChecked = true,
            typedAnswerCorrect = false,
        )

        assertThat(policy.enabledGrades).containsExactly(
            ReviewGrade.AGAIN,
            ReviewGrade.HARD,
        )
    }

    @Test
    fun `manual cards keep all grades enabled`() {
        val policy = productionCard(evaluationMode = EvaluationMode.ManualSelfCheck).gradePolicy(
            typedAnswerChecked = false,
            typedAnswerCorrect = false,
        )

        assertThat(policy.enabledGrades).containsExactlyElementsIn(ReviewGrade.entries)
    }

    private fun productionCard(
        evaluationMode: EvaluationMode = EvaluationMode.Exact,
    ) = GeneratedStudyCard(
        cardId = "card-1",
        cardType = StudyCardType.Production,
        prompt = "casualidad afortunada",
        expectedAnswer = "serendipity",
        evaluationMode = evaluationMode,
    )
}
