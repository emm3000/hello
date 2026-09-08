package com.emm.data.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.RegisterPreference

private val dailyStandupPatternSpecs: List<SentencePatternSpec> = listOf(
    SentencePatternSpec(
        expression = "Yesterday I fixed it, today I'm testing it",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "the standup rhythm: past simple for yesterday, present continuous for today.",
        meaningEs = "ayer lo arreglé, hoy lo estoy probando",
        whyUseful = "it is the two-tense shape every turn in the meeting runs on.",
        example = "Yesterday I fixed it, today I'm testing it on a real device.",
        translation = "Ayer lo arreglé, hoy lo estoy probando en un dispositivo real.",
        commonMistake =
        "saying 'yesterday I have fixed it' for 'ayer lo he arreglado' — 'yesterday' takes the " +
            "past simple, never the present perfect.",
        confusableWith = listOf("ayer lo he arreglado → yesterday I fixed it"),
        collocations = emptyList(),
        usagePattern = "yesterday + [past simple], today + [be + -ing]",
        clozeSentence = "Yesterday I ___ it, today I'm testing it.",
        clozeAnswer = "fixed",
        sourceContext = "Daily: contar tu avance de ayer y de hoy",
        acceptedAnswers = listOf("Yesterday I fixed it, today I am testing it"),
    ),
    SentencePatternSpec(
        expression = "I'm waiting on the API key from Ana",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "how to name the thing you need and the person who owes it to you.",
        meaningEs = "estoy esperando la clave de Ana",
        whyUseful = "'esperar' takes a direct object in Spanish and a preposition in English.",
        example = "I'm waiting on the API key from Ana to finish it.",
        translation = "Estoy esperando que Ana me pase la clave de la API para terminarlo.",
        commonMistake =
        "saying 'I'm waiting Ana' for 'estoy esperando a Ana' — 'wait' needs 'on' or 'for' " +
            "before what you are waiting for.",
        confusableWith = listOf("esperar algo de alguien → wait on something from someone"),
        collocations = emptyList(),
        usagePattern = "be waiting on + [thing] + from + [person]",
        clozeSentence = "I'm waiting ___ the API key from Ana.",
        clozeAnswer = "on",
        sourceContext = "Daily: decir de quién dependes",
        acceptedAnswers = listOf("I am waiting on the API key from Ana"),
    ),
    SentencePatternSpec(
        expression = "It should be ready by Thursday",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "a deadline takes 'by' plus the day, not 'until'.",
        meaningEs = "debería estar listo para el jueves",
        whyUseful = "'para el jueves' comes out as 'until Thursday' and promises the wrong thing.",
        example = "It should be ready by Thursday, unless the review takes longer.",
        translation = "Debería estar listo para el jueves, salvo que la revisión demore más.",
        commonMistake =
        "saying 'ready until Thursday' for 'para el jueves' — a deadline takes 'by'; " +
            "'until' means all the time before it.",
        confusableWith = listOf("para el jueves → by Thursday", "hasta el jueves → until Thursday"),
        collocations = emptyList(),
        usagePattern = "should be ready by + [day]",
        clozeSentence = "It should be ready ___ Thursday.",
        clozeAnswer = "by",
        sourceContext = "Daily: dar una fecha de entrega",
    ),
    SentencePatternSpec(
        expression = "Once the PR is merged, I'll deploy",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "'once' plus a present verb for a future condition, with no 'will' inside it.",
        meaningEs = "en cuanto se haga el merge, despliego",
        whyUseful = "Spanish uses the subjunctive here and English keeps a plain present.",
        example = "Once the PR is merged, I'll deploy it to staging.",
        translation = "En cuanto hagan el merge del PR, lo despliego a staging.",
        commonMistake =
        "saying 'once the PR will be merged' for 'en cuanto se haga el merge' — the clause " +
            "after 'once' stays in the present.",
        confusableWith = listOf("en cuanto + subjuntivo → once + presente"),
        collocations = emptyList(),
        usagePattern = "once + [present clause], + [will clause]",
        clozeSentence = "Once the PR ___ merged, I'll deploy it.",
        clozeAnswer = "is",
        sourceContext = "Daily: encadenar tu tarea con la de otro",
        acceptedAnswers = listOf("Once the PR is merged, I will deploy"),
    ),
    SentencePatternSpec(
        expression = "I finished writing the migration",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "'finish' is followed by an -ing verb, never by 'to' plus a verb.",
        meaningEs = "terminé de escribir la migración",
        whyUseful = "'terminé de escribir' pulls 'finished to write' out of almost everyone.",
        example = "I finished writing the migration late yesterday.",
        translation = "Terminé de escribir la migración ayer por la noche.",
        commonMistake =
        "saying 'I finished to write the migration' for 'terminé de escribir' — 'finish' takes " +
            "the -ing form of the next verb.",
        confusableWith = listOf("terminar de + infinitivo → finish + -ing"),
        collocations = emptyList(),
        usagePattern = "finish + [verb]-ing",
        clozeSentence = "I finished ___ the migration late yesterday.",
        clozeAnswer = "writing",
        sourceContext = "Daily: reportar lo que terminaste",
    ),
    SentencePatternSpec(
        expression = "Can you explain the flow to me?",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        definitionEn = "'explain' puts the person after 'to', never straight after the verb.",
        meaningEs = "¿me puedes explicar el flujo?",
        whyUseful = "'explícame' turns into 'explain me', which English never allows.",
        example = "Can you explain the flow to me after the standup?",
        translation = "¿Me puedes explicar el flujo después de la reunión?",
        commonMistake =
        "saying 'can you explain me the flow' for 'explícame el flujo' — the person needs 'to': " +
            "'explain the flow to me'.",
        confusableWith = listOf("explícame → explain it to me"),
        collocations = emptyList(),
        usagePattern = "explain + [thing] + to + [person]",
        clozeSentence = "Can you explain the flow ___ me?",
        clozeAnswer = "to",
        sourceContext = "Daily: pedir una explicación",
        register = RegisterPreference.Formal,
    ),
)

internal val dailyStandupPatterns: List<GeneratedLearningNote> =
    dailyStandupPatternSpecs.map { spec -> spec.toNote(DAILY_STANDUP_DECK_ID) }
