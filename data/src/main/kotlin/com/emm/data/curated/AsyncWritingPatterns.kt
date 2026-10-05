package com.emm.data.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LevelBand

private val asyncWritingPatternSpecs: List<SentencePatternSpec> = listOf(
    SentencePatternSpec(
        expression = "Could you review my PR when you can?",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "a polite request for a code review that does not rush the reviewer.",
        meaningEs = "¿podrías revisar mi PR cuando puedas?",
        whyUseful = "it asks for a review without pressure, which matters on a remote team.",
        example = "Hi Lucas, could you review my PR when you can? It's a small one.",
        translation = "Hola Lucas, ¿podrías revisar mi PR cuando puedas? Es uno pequeño.",
        commonMistake =
        "writing 'could you revise my PR?' for '¿podrías revisar mi PR?' — 'revise' means " +
            "rewriting a text; code gets a 'review'.",
        confusableWith = listOf("revisar (código) → review", "corregir un texto → revise"),
        collocations = emptyList(),
        usagePattern = "Could you review + [my PR / this] + when you can?",
        clozeSentence = "Could you ___ my PR when you can?",
        clozeAnswer = "review",
        sourceContext = "Async: mensaje en Slack",
        acceptedAnswers = listOf(
            "Could you review my PR when you have a chance?",
            "Could you review my PR when you can",
        ),
    ),
    SentencePatternSpec(
        expression = "I can't reproduce the bug on my machine",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "a way to say the error does not happen when you try it.",
        meaningEs = "no puedo reproducir el bug en mi máquina",
        whyUseful = "it is the first reply to many bug reports, and it asks for details without blame.",
        example = "I can't reproduce the bug on my machine. Which browser are you using?",
        translation = "No puedo reproducir el bug en mi máquina. ¿Qué navegador usas?",
        commonMistake =
        "writing 'the bug doesn't come out to me' for 'no me sale el bug' — 'salir' is not " +
            "'come out' here; say 'I can't reproduce the bug'.",
        confusableWith = listOf("no me sale el error → I can't reproduce it", "salir (de un lugar) → go out"),
        collocations = emptyList(),
        usagePattern = "I can't reproduce + [the bug / the issue] + on my machine",
        clozeSentence = "I can't ___ the bug on my machine.",
        clozeAnswer = "reproduce",
        sourceContext = "Async: respuesta a un reporte de bug",
        acceptedAnswers = listOf("I cannot reproduce the bug on my machine"),
    ),
    SentencePatternSpec(
        expression = "Just to confirm, the deadline is Friday, right?",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "a polite way to check that you understood a date or a decision.",
        meaningEs = "para confirmar, ¿la entrega es el viernes?",
        whyUseful = "in async work a wrong assumption costs a day, so checking in writing is normal.",
        example = "Just to confirm, the deadline is Friday, right? I want to plan my week.",
        translation = "Para confirmar, ¿la entrega es el viernes? Quiero planificar mi semana.",
        commonMistake =
        "writing 'only for confirm' for 'solo para confirmar' — after 'to' comes the verb: " +
            "'just to confirm'.",
        confusableWith = listOf("solo para confirmar → just to confirm", "fecha de entrega → deadline"),
        collocations = emptyList(),
        usagePattern = "Just to confirm, + [fact], right?",
        clozeSentence = "Just to ___, the deadline is Friday, right?",
        clozeAnswer = "confirm",
        sourceContext = "Async: mensaje en Slack",
        acceptedAnswers = listOf("Just to confirm, the deadline is Friday?"),
    ),
    SentencePatternSpec(
        expression = "I've updated the PR based on your comments",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "a message telling the reviewer you made the changes they asked for.",
        meaningEs = "actualicé el PR según tus comentarios",
        whyUseful = "it is the message that sends a PR back to the reviewer.",
        example = "I've updated the PR based on your comments. Could you take another look?",
        translation = "Actualicé el PR según tus comentarios. ¿Podrías revisarlo otra vez?",
        commonMistake =
        "writing 'I updated the PR according your comments' for 'según tus comentarios' — " +
            "'according' needs 'to'; the natural phrase is 'based on your comments'.",
        confusableWith = listOf("según tus comentarios → based on your comments", "según (una fuente) → according to"),
        collocations = emptyList(),
        usagePattern = "I've updated + [the PR / the ticket] + based on your comments",
        clozeSentence = "I've updated the PR ___ on your comments.",
        clozeAnswer = "based",
        sourceContext = "Async: respuesta a una review",
        acceptedAnswers = listOf("I have updated the PR based on your comments"),
    ),
    SentencePatternSpec(
        expression = "Let me know if you have any questions",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "a friendly line that closes a message and invites questions.",
        meaningEs = "avísame si tienes alguna duda",
        whyUseful = "it closes almost every handoff message in Slack and email.",
        example = "The setup steps are in the README. Let me know if you have any questions.",
        translation = "Los pasos de instalación están en el README. Avísame si tienes alguna duda.",
        commonMistake =
        "writing 'if you have any doubt' for 'si tienes alguna duda' — 'doubt' means you do not " +
            "believe something; a 'duda' you ask about is a 'question'.",
        confusableWith = listOf("duda (pregunta) → question", "duda (desconfianza) → doubt"),
        collocations = emptyList(),
        usagePattern = "Let me know if + [you have any questions / you need anything]",
        clozeSentence = "Let me know if you have any ___.",
        clozeAnswer = "questions",
        sourceContext = "Async: cierre de un mensaje",
        acceptedAnswers = listOf("Let me know if you have questions"),
    ),
    SentencePatternSpec(
        expression = "Could you share the error message?",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "a polite request for the exact text of an error.",
        meaningEs = "¿podrías pasarme el mensaje de error?",
        whyUseful = "you cannot help with a bug without the error, and this asks for it politely.",
        example = "Could you share the error message? A screenshot is fine too.",
        translation = "¿Podrías pasarme el mensaje de error? Un screenshot también sirve.",
        commonMistake =
        "writing 'can you pass me the error?' for '¿me pasas el error?' — 'pass' is for objects " +
            "you hand over; for a text or a file, 'share' or 'send'.",
        confusableWith = listOf("pasar (un archivo) → share / send", "pasar (un objeto) → pass"),
        collocations = emptyList(),
        usagePattern = "Could you share + [the error message / the logs]?",
        clozeSentence = "Could you ___ the error message?",
        clozeAnswer = "share",
        sourceContext = "Async: respuesta a un reporte de bug",
        acceptedAnswers = listOf("Can you share the error message?"),
    ),
)

internal val asyncWritingPatterns: List<GeneratedLearningNote> =
    asyncWritingPatternSpecs.map { spec -> spec.toNote(ASYNC_WRITING_DECK_ID) }
