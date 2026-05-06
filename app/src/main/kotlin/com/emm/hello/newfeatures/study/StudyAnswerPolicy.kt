package com.emm.hello.newfeatures.study

import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.study.ReviewGrade

private val allReviewGrades = ReviewGrade.entries.toSet()

internal data class ReviewGradePolicy(
    val enabledGrades: Set<ReviewGrade> = allReviewGrades,
    val guidance: String = "",
)

internal val GeneratedStudyCard.needsTypedAnswer: Boolean
    get() = evaluationMode == EvaluationMode.Exact || evaluationMode == EvaluationMode.FlexibleText

internal fun GeneratedStudyCard.gradePolicy(
    typedAnswerChecked: Boolean,
    typedAnswerCorrect: Boolean,
): ReviewGradePolicy {
    if (!needsTypedAnswer || !typedAnswerChecked) {
        return ReviewGradePolicy()
    }

    return if (typedAnswerCorrect) {
        ReviewGradePolicy(
            enabledGrades = setOf(ReviewGrade.HARD, ReviewGrade.GOOD, ReviewGrade.EASY),
            guidance = "Como la respuesta coincidió, la nota mínima disponible es Hard.",
        )
    } else {
        ReviewGradePolicy(
            enabledGrades = setOf(ReviewGrade.AGAIN, ReviewGrade.HARD),
            guidance = "Como la respuesta no coincidió, Good y Easy quedan bloqueadas.",
        )
    }
}

internal fun matchesTypedAnswer(
    evaluationMode: EvaluationMode,
    typedAnswer: String,
    expectedAnswer: String,
    acceptedAnswers: List<String>,
): Boolean {
    val normalizedTypedAnswer = typedAnswer.normalizeForComparison()
    if (normalizedTypedAnswer.isBlank()) return false

    val candidates = buildList {
        add(expectedAnswer)
        addAll(acceptedAnswers)
    }.map(String::normalizeForComparison)
        .filter(String::isNotBlank)

    return when (evaluationMode) {
        EvaluationMode.Exact -> candidates.any { it == normalizedTypedAnswer }
        EvaluationMode.FlexibleText -> candidates.any { candidate ->
            normalizedTypedAnswer == candidate ||
                normalizedTypedAnswer.contains(candidate) ||
                candidate.contains(normalizedTypedAnswer)
        }
        EvaluationMode.ManualSelfCheck -> false
    }
}

private fun String.normalizeForComparison(): String {
    return lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
}
