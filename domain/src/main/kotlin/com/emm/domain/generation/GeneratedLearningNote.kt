package com.emm.domain.generation

import com.emm.domain.flashcard.DefinitionEn
import com.emm.domain.flashcard.Expression
import com.emm.domain.flashcard.IntendedMeaningEs

data class GeneratedLearningNote(
    val noteId: String,
    val noteType: LearningNoteType,
    val expression: String,
    val intendedMeaningEs: String,
    val simpleDefinitionEn: String,
    val partOfSpeech: PartOfSpeechTag,
    val register: RegisterPreference,
    val levelBand: LevelBand,
    val domain: LearningDomain,
    val whyUseful: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val cards: List<GeneratedStudyCard>,
    val qualityChecks: List<GeneratedNoteQualityCheck>,
    val lemma: String = "",
    val ipa: String = "",
    val usagePattern: String = "",
    val irregularForms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val commonMistake: String = "",
    val confusableWith: List<String> = emptyList(),
    val clozeSentence: String = "",
    val sourceContext: String = "",
    val warnings: List<String> = emptyList(),
) {

    val expressionValue: Expression?
        get() = Expression.fromOrNull(expression)

    val intendedMeaningEsValue: IntendedMeaningEs?
        get() = IntendedMeaningEs.fromOrNull(intendedMeaningEs)

    val simpleDefinitionEnValue: DefinitionEn?
        get() = DefinitionEn.fromOrNull(simpleDefinitionEn)

    fun requireExpression(): Expression {
        return expressionValue ?: throw invalidSemanticField("expression")
    }

    fun requireIntendedMeaningEs(): IntendedMeaningEs {
        return intendedMeaningEsValue ?: throw invalidSemanticField("intendedMeaningEs")
    }

    fun requireSimpleDefinitionEn(): DefinitionEn {
        return simpleDefinitionEnValue ?: throw invalidSemanticField("simpleDefinitionEn")
    }

    companion object {
        fun fromSemanticCore(
            noteId: String,
            noteType: LearningNoteType,
            expression: Expression,
            intendedMeaningEs: IntendedMeaningEs,
            simpleDefinitionEn: DefinitionEn,
            partOfSpeech: PartOfSpeechTag,
            register: RegisterPreference,
            levelBand: LevelBand,
            domain: LearningDomain,
            whyUseful: String,
            exampleSentence: String,
            exampleTranslation: String,
            cards: List<GeneratedStudyCard>,
            qualityChecks: List<GeneratedNoteQualityCheck>,
            lemma: String = "",
            ipa: String = "",
            usagePattern: String = "",
            irregularForms: List<String> = emptyList(),
            collocations: List<String> = emptyList(),
            commonMistake: String = "",
            confusableWith: List<String> = emptyList(),
            clozeSentence: String = "",
            sourceContext: String = "",
            warnings: List<String> = emptyList(),
        ): GeneratedLearningNote {
            return GeneratedLearningNote(
                noteId = noteId,
                noteType = noteType,
                expression = expression.value,
                intendedMeaningEs = intendedMeaningEs.value,
                simpleDefinitionEn = simpleDefinitionEn.value,
                partOfSpeech = partOfSpeech,
                register = register,
                levelBand = levelBand,
                domain = domain,
                whyUseful = whyUseful,
                exampleSentence = exampleSentence,
                exampleTranslation = exampleTranslation,
                cards = cards,
                qualityChecks = qualityChecks,
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
        }
    }
}

private fun invalidSemanticField(fieldName: String): IllegalStateException {
    return IllegalStateException("GeneratedLearningNote.$fieldName must be semantically valid before this operation.")
}
