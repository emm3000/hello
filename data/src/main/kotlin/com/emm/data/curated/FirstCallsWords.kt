package com.emm.data.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag

private val firstCallsWordSpecs: List<WordSpec> = listOf(
    WordSpec(
        expression = "already",
        partOfSpeech = PartOfSpeechTag.Adverb,
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        ipa = "ɔːlˈredi",
        definitionEn = "before now, sooner than someone expected.",
        meaningEs = "ya",
        whyUseful = "it is the quickest way to say a task is done, and Spanish puts 'ya' in the wrong place.",
        example = "I already pushed the fix.",
        translation = "Ya subí el arreglo.",
        commonMistake =
        "saying 'already I pushed it' for 'ya lo subí' — 'already' goes after the subject: " +
            "'I already pushed it'.",
        confusableWith = listOf("ya (terminado) → already", "ya no → not anymore"),
        sourceContext = "Llamada: cuando ya terminaste algo",
        collocations = listOf("already done", "already pushed"),
    ),
    WordSpec(
        expression = "yet",
        partOfSpeech = PartOfSpeechTag.Adverb,
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        ipa = "jet",
        definitionEn = "until now; used in negative sentences and in questions.",
        meaningEs = "todavía / ya (en negativas y preguntas)",
        whyUseful = "'not yet' is the honest short answer when someone asks if you finished.",
        example = "I haven't tested it yet.",
        translation = "Todavía no lo he probado.",
        commonMistake =
        "saying 'I didn't test it still' for 'todavía no lo probé' — in a negative sentence " +
            "the word is 'yet', at the end: 'I haven't tested it yet'.",
        confusableWith = listOf("todavía no → not yet", "¿ya está? → is it done yet?"),
        sourceContext = "Llamada: cuando aún no terminas algo",
        collocations = listOf("not yet", "done yet"),
        acceptedAnswers = listOf("not yet"),
    ),
    WordSpec(
        expression = "still",
        partOfSpeech = PartOfSpeechTag.Adverb,
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        ipa = "stɪl",
        definitionEn = "continuing; something has not stopped or changed.",
        meaningEs = "aún, todavía (sigue pasando)",
        whyUseful = "it tells the team you are in the middle of a task, not stuck and not finished.",
        example = "I'm still working on it.",
        translation = "Todavía estoy trabajando en eso.",
        commonMistake =
        "saying 'I'm working on it yet' for 'todavía estoy en eso' — for something that continues, " +
            "use 'still', before the verb.",
        confusableWith = listOf("todavía (sigue) → still", "todavía no → not yet"),
        sourceContext = "Llamada: cuando sigues trabajando en algo",
        collocations = listOf("still working on it", "still broken"),
    ),
    WordSpec(
        expression = "wait",
        partOfSpeech = PartOfSpeechTag.Verb,
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        ipa = "weɪt",
        definitionEn = "to stay until someone arrives or something happens.",
        meaningEs = "esperar (a alguien o algo)",
        whyUseful = "calls often start late, and you need to say who or what you are waiting for.",
        example = "Let's wait for Ana before we start.",
        translation = "Esperemos a Ana antes de empezar.",
        commonMistake =
        "saying 'let's wait Ana' for 'esperemos a Ana' — 'wait' needs 'for' before the person " +
            "or thing; and it never means 'tener esperanza'.",
        confusableWith = listOf("esperar a alguien → wait for", "esperar que pase → expect", "tener esperanza → hope"),
        sourceContext = "Llamada: cuando falta alguien para empezar",
        collocations = listOf("wait for", "wait a minute"),
        productionHint = "a alguien o algo",
    ),
    WordSpec(
        expression = "expect",
        partOfSpeech = PartOfSpeechTag.Verb,
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        ipa = "ɪkˈspekt",
        definitionEn = "to think that something will probably happen.",
        meaningEs = "esperar (que algo pase)",
        whyUseful = "it is how you give a date without promising it.",
        example = "I expect it to be ready on Friday.",
        translation = "Espero que esté listo el viernes.",
        commonMistake =
        "saying 'I wait it is ready on Friday' for 'espero que esté listo' — when you think " +
            "something will happen, the verb is 'expect', not 'wait'.",
        confusableWith = listOf("esperar que pase → expect", "esperar a alguien → wait for", "ojalá → hope"),
        sourceContext = "Llamada: cuando das una fecha probable",
        collocations = listOf("expect it to be", "I expect so"),
        productionHint = "que algo pase",
    ),
    WordSpec(
        expression = "sure",
        partOfSpeech = PartOfSpeechTag.Interjection,
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        ipa = "ʃʊr",
        definitionEn = "a short, friendly yes to a request.",
        meaningEs = "claro, seguro",
        whyUseful = "it is the fastest friendly yes when someone asks you for something in a call.",
        example = "Sure, I can share my screen.",
        translation = "Claro, puedo compartir mi pantalla.",
        commonMistake =
        "saying 'of course yes' or 'clear' for 'claro que sí' — the short reply is 'sure'; " +
            "'clear' never means 'yes'.",
        confusableWith = listOf("claro → sure", "claro (transparente) → clear"),
        sourceContext = "Llamada: cuando te piden algo y aceptas",
        collocations = listOf("sure thing", "sure, no problem"),
    ),
)

internal val firstCallsWords: List<GeneratedLearningNote> =
    firstCallsWordSpecs.map { spec -> spec.toNote(FIRST_CALLS_DECK_ID) }
