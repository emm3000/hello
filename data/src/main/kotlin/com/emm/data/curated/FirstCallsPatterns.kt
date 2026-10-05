package com.emm.data.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LevelBand

private val firstCallsPatternSpecs: List<SentencePatternSpec> = listOf(
    SentencePatternSpec(
        expression = "Sorry, I didn't catch that",
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        definitionEn = "a polite way to say you did not hear or understand what was said.",
        meaningEs = "perdón, no te entendí",
        whyUseful = "it is the sentence you will need most in your first calls, and it sounds natural.",
        example = "Sorry, I didn't catch that. The audio cut out.",
        translation = "Perdón, no te entendí. Se cortó el audio.",
        commonMistake =
        "saying 'I didn't understand you' for 'no te entendí' — it can sound like the speaker is " +
            "unclear; 'I didn't catch that' blames the audio, not them.",
        confusableWith = listOf("no te entendí → I didn't catch that", "no te escuché → I didn't hear you"),
        collocations = emptyList(),
        usagePattern = "Sorry, I didn't catch + [that / your name / the last part]",
        clozeSentence = "Sorry, I didn't ___ that.",
        clozeAnswer = "catch",
        sourceContext = "Llamada: cuando no entendiste algo",
        acceptedAnswers = listOf("Sorry, I did not catch that"),
    ),
    SentencePatternSpec(
        expression = "Could you repeat that, please?",
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        definitionEn = "a polite request to hear something again.",
        meaningEs = "¿podrías repetirlo, por favor?",
        whyUseful = "asking again is normal in a call, and this is the polite form.",
        example = "Could you repeat that, please? I missed the date.",
        translation = "¿Podrías repetirlo, por favor? No escuché la fecha.",
        commonMistake =
        "saying 'can you repeat me?' for '¿me repites?' — 'repeat' takes the thing, not the " +
            "person: 'repeat that'.",
        confusableWith = listOf("¿me repites? → could you repeat that?", "repetir → repeat"),
        collocations = emptyList(),
        usagePattern = "Could you + [verb] + that, please?",
        clozeSentence = "Could you ___ that, please?",
        clozeAnswer = "repeat",
        sourceContext = "Llamada: cuando necesitas que lo digan otra vez",
        acceptedAnswers = listOf("Could you repeat that please?", "Could you repeat that, please"),
    ),
    SentencePatternSpec(
        expression = "Could you speak more slowly, please?",
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        definitionEn = "a polite request for someone to talk at a slower speed.",
        meaningEs = "¿podrías hablar más despacio, por favor?",
        whyUseful = "native speakers talk fast on calls, and asking is better than guessing.",
        example = "Could you speak more slowly, please? My English is still basic.",
        translation = "¿Podrías hablar más despacio, por favor? Mi inglés todavía es básico.",
        commonMistake =
        "saying 'speak more slow' or 'speak more despacio' — after 'speak' you need the adverb " +
            "'slowly'.",
        confusableWith = listOf("despacio → slowly", "lento (adjetivo) → slow"),
        collocations = emptyList(),
        usagePattern = "Could you speak more + [adverb], please?",
        clozeSentence = "Could you speak more ___, please?",
        clozeAnswer = "slowly",
        sourceContext = "Llamada: cuando hablan muy rápido",
        acceptedAnswers = listOf("Could you speak more slowly please?", "Could you speak slower, please?"),
    ),
    SentencePatternSpec(
        expression = "What do you mean by \"deploy\"?",
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        definitionEn = "a question to ask what someone means by one specific word.",
        meaningEs = "¿a qué te refieres con \"deploy\"?",
        whyUseful = "it lets you ask about one word without stopping the whole call.",
        example = "What do you mean by \"deploy\"? To staging or to production?",
        translation = "¿A qué te refieres con \"deploy\"? ¿A staging o a producción?",
        commonMistake =
        "saying 'what do you refer with deploy?' for '¿a qué te refieres con...?' — the natural " +
            "question is 'what do you mean by...?'.",
        confusableWith = listOf("¿a qué te refieres con...? → what do you mean by...?", "referirse a → refer to"),
        collocations = emptyList(),
        usagePattern = "What do you mean by + [word]?",
        clozeSentence = "What do you mean ___ \"deploy\"?",
        clozeAnswer = "by",
        sourceContext = "Llamada: cuando no conoces una palabra",
        acceptedAnswers = listOf("What do you mean by deploy?"),
    ),
    SentencePatternSpec(
        expression = "How do you say \"plazo\" in English?",
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        definitionEn = "a question to ask for the English word for a Spanish one.",
        meaningEs = "¿cómo se dice \"plazo\" en inglés?",
        whyUseful = "when a word is missing, asking for it keeps the call moving.",
        example = "How do you say \"plazo\" in English? The date we need to finish.",
        translation = "¿Cómo se dice \"plazo\" en inglés? La fecha en que tenemos que terminar.",
        commonMistake =
        "saying 'how is said plazo in English?' for '¿cómo se dice?' — English uses 'you': " +
            "'how do you say...?'.",
        confusableWith = listOf("¿cómo se dice...? → how do you say...?", "plazo → deadline"),
        collocations = emptyList(),
        usagePattern = "How do you say + [word] + in English?",
        clozeSentence = "How do you ___ \"plazo\" in English?",
        clozeAnswer = "say",
        sourceContext = "Llamada: cuando te falta una palabra",
        acceptedAnswers = listOf("How do you say plazo in English?"),
    ),
    SentencePatternSpec(
        expression = "Let me check and get back to you",
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        definitionEn = "a promise to find the answer and reply to someone later.",
        meaningEs = "déjame revisarlo y te respondo",
        whyUseful = "it is the honest answer when you do not know something during a call.",
        example = "Let me check and get back to you after lunch.",
        translation = "Déjame revisarlo y te respondo después del almuerzo.",
        commonMistake =
        "saying 'I return to you' for 'te vuelvo a escribir' — to reply later, " +
            "the phrase is 'get back to you'.",
        confusableWith = listOf("te respondo luego → I'll get back to you", "volver a un lugar → return"),
        collocations = emptyList(),
        usagePattern = "Let me + [verb] + and get back to you",
        clozeSentence = "Let me check and ___ back to you.",
        clozeAnswer = "get",
        sourceContext = "Llamada: cuando no sabes la respuesta",
        acceptedAnswers = listOf("Let me check and I'll get back to you"),
    ),
    SentencePatternSpec(
        expression = "I'm working on the login bug",
        levelBand = LevelBand.A1_A2,
        domain = LearningDomain.Work,
        definitionEn = "how to say what task you are doing at the moment.",
        meaningEs = "estoy trabajando en el bug del login",
        whyUseful = "someone will ask what you are doing, and this is the answer shape.",
        example = "I'm working on the login bug, it should be done today.",
        translation = "Estoy trabajando en el bug del login, debería estar listo hoy.",
        commonMistake =
        "saying 'I'm working in the login bug' for 'trabajando en' — with a task, " +
            "'work' takes 'on', not 'in'.",
        confusableWith = listOf("trabajar en (una tarea) → work on", "trabajar en (una empresa) → work at"),
        collocations = emptyList(),
        usagePattern = "I'm working on + [task]",
        clozeSentence = "I'm working ___ the login bug.",
        clozeAnswer = "on",
        sourceContext = "Llamada: cuando dices en qué estás",
        acceptedAnswers = listOf("I am working on the login bug"),
    ),
)

internal val firstCallsPatterns: List<GeneratedLearningNote> =
    firstCallsPatternSpecs.map { spec -> spec.toNote(FIRST_CALLS_DECK_ID) }
