package com.emm.data.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag
import com.emm.domain.generation.RegisterPreference

private val asyncWritingWordSpecs: List<WordSpec> = listOf(
    WordSpec(
        expression = "approve",
        partOfSpeech = PartOfSpeechTag.Verb,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "əˈpruːv",
        definitionEn = "to say officially that a change is good enough to merge.",
        meaningEs = "aprobar (un PR)",
        whyUseful = "every PR waits for someone to approve it, and you will ask for that every day.",
        example = "Can you approve my PR? The tests pass.",
        translation = "¿Puedes aprobar mi PR? Los tests pasan.",
        commonMistake =
        "writing 'can you approve me the PR?' for '¿me apruebas el PR?' — 'approve' takes the " +
            "thing, not the person: 'approve my PR'.",
        confusableWith = listOf("aprobar (un PR) → approve", "aprobar (un examen) → pass"),
        sourceContext = "Async: cuando pides una aprobación",
        collocations = listOf("approve the PR", "approved with comments"),
        acceptedAnswers = listOf("approve the PR"),
    ),
    WordSpec(
        expression = "merge",
        partOfSpeech = PartOfSpeechTag.Verb,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "mɜːrdʒ",
        definitionEn = "to join the changes from one branch into another.",
        meaningEs = "integrar, fusionar (una rama)",
        whyUseful = "it is the last step of every PR, and it appears in every Slack thread about a release.",
        example = "I'll merge it into main after lunch.",
        translation = "Lo integro a main después del almuerzo.",
        commonMistake =
        "writing 'I'll fusion the branch' for 'fusiono la rama' — 'fusion' is only a noun; " +
            "the verb is 'merge'.",
        confusableWith = listOf("fusionar (ramas) → merge", "fusión (nuclear) → fusion"),
        sourceContext = "Async: comentario en un PR",
        collocations = listOf("merge into main", "merge conflict"),
    ),
    WordSpec(
        expression = "nit",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "nɪt",
        definitionEn = "a very small review comment that does not block the change.",
        meaningEs = "detalle menor (en una review)",
        whyUseful = "reviewers start small comments with 'nit:', and you need to know it is not a blocker.",
        example = "Nit: this variable name could be clearer.",
        translation = "Detalle menor: el nombre de esta variable podría ser más claro.",
        commonMistake =
        "writing 'little detail:' or 'small observation:' for 'detalle menor' — reviewers " +
            "write 'nit:', and it marks the comment as optional.",
        confusableWith = listOf("detalle menor (en una review) → nit", "detalle (en general) → detail"),
        sourceContext = "Async: comentario en un PR",
        register = RegisterPreference.Casual,
        collocations = listOf("just a nit", "nit: rename this"),
    ),
    WordSpec(
        expression = "typo",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈtaɪpoʊ",
        definitionEn = "a small mistake made when typing.",
        meaningEs = "error de tipeo",
        whyUseful = "many small PRs and commits fix typos, and you will name them in one word.",
        example = "Fixed a typo in the README.",
        translation = "Corregí un error de tipeo en el README.",
        commonMistake =
        "writing 'a typing error' or 'a tipeo' for 'un error de tipeo' — the short word is " +
            "'typo', and you 'fix' it.",
        confusableWith = listOf("error de tipeo → typo", "error en el código → bug"),
        sourceContext = "Async: mensaje de un commit",
        collocations = listOf("fix a typo", "small typo"),
    ),
    WordSpec(
        expression = "workaround",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈwɜːrkəraʊnd",
        definitionEn = "a temporary way to avoid a problem without really fixing it.",
        meaningEs = "solución provisional",
        whyUseful = "when you cannot fix the root cause today, you tell the team there is a workaround.",
        example = "For now, the workaround is to clear the cache.",
        translation = "Por ahora, la solución provisional es limpiar la caché.",
        commonMistake =
        "writing 'a temporal solution' for 'una solución temporal' — 'temporal' is about time " +
            "in general; say 'a temporary fix' or 'a workaround'.",
        confusableWith = listOf("solución provisional → workaround", "temporal (provisional) → temporary"),
        sourceContext = "Async: comentario en un ticket",
        collocations = listOf("a quick workaround", "find a workaround"),
        acceptedAnswers = listOf("a workaround"),
    ),
    WordSpec(
        expression = "issue",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈɪʃuː",
        definitionEn = "a problem, or a ticket that describes one.",
        meaningEs = "problema, incidencia",
        whyUseful = "tickets on GitHub and Jira are called issues, and 'there is an issue' reports a problem.",
        example = "I opened an issue for the login crash.",
        translation = "Abrí una incidencia por el crash del login.",
        commonMistake =
        "writing 'we need to talk about an issue' for 'tenemos que hablar de un tema' — " +
            "'issue' sounds like a problem; for a neutral subject, say 'topic' or 'thing'.",
        confusableWith = listOf("problema (técnico) → issue", "tema (de conversación) → topic"),
        sourceContext = "Async: cuando reportas un problema en un ticket",
        collocations = listOf("open an issue", "known issue"),
    ),
    WordSpec(
        expression = "available",
        partOfSpeech = PartOfSpeechTag.Adjective,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "əˈveɪləbl",
        definitionEn = "free to talk or help, or ready to be used.",
        meaningEs = "disponible",
        whyUseful = "you ask it before every quick call, and your Slack status says it for you.",
        example = "Are you available for a quick call?",
        translation = "¿Estás disponible para una llamada rápida?",
        commonMistake =
        "writing 'I'm disposable' for 'estoy disponible' — 'disposable' means 'desechable', " +
            "like a plastic cup; the word is 'available'.",
        confusableWith = listOf("disponible → available", "desechable → disposable"),
        sourceContext = "Async: mensaje en Slack",
        collocations = listOf("available for a call", "not available today"),
    ),
)

internal val asyncWritingWords: List<GeneratedLearningNote> =
    asyncWritingWordSpecs.map { spec -> spec.toNote(ASYNC_WRITING_DECK_ID) }
