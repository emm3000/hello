package com.emm.data.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.RegisterPreference

private val dailyStandupPhraseSpecs: List<CollocationSpec> = listOf(
    CollocationSpec(
        expression = "I'm blocked on",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "aɪm blɑːkt ɑːn",
        definitionEn = "you cannot move forward until someone or something unblocks you.",
        meaningEs = "estoy bloqueado con",
        whyUseful = "it is the standup sentence that actually gets you help, and the preposition is fixed.",
        example = "I'm blocked on the review of the auth pull request.",
        translation = "Estoy bloqueado con la revisión del pull request de auth.",
        commonMistake =
        "saying 'I'm blocked with the review' for 'bloqueado con' — the preposition after " +
            "'blocked' is 'on'.",
        confusableWith = listOf("estoy bloqueado con → I'm blocked on"),
        collocations = listOf("blocked on a review", "blocked on a dependency"),
        usagePattern = "be blocked on + [thing or person]",
        sourceContext = "Daily: reportar un bloqueo",
        clozeAnswer = "blocked on",
        acceptedAnswers = listOf("I am blocked on"),
    ),
    CollocationSpec(
        expression = "no updates from my side",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "noʊ ˈʌpdeɪts frʌm maɪ saɪd",
        definitionEn = "nothing has changed since the last time you reported.",
        meaningEs = "sin novedades de mi parte",
        whyUseful = "it closes your turn honestly in three seconds instead of inventing progress.",
        example = "No updates from my side, same as yesterday.",
        translation = "Sin novedades de mi parte, igual que ayer.",
        commonMistake =
        "saying 'no news of my part' for 'de mi parte' — the standup phrase is " +
            "'no updates from my side'.",
        confusableWith = listOf("de mi parte → from my side", "sin novedades → no updates"),
        collocations = listOf("no updates from my side", "nothing from my side"),
        usagePattern = "no updates from + [my / his / her] + side",
        sourceContext = "Daily: cerrar tu turno sin novedades",
        clozeAnswer = "from my side",
    ),
    CollocationSpec(
        expression = "let's take it offline",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "lets teɪk ɪt ˈɔːflaɪn",
        definitionEn = "to move a long discussion out of the meeting into a smaller group.",
        meaningEs = "hablémoslo después, fuera de la reunión",
        whyUseful = "it stops a two-person debate from eating everyone else's standup.",
        example = "That's a long one, let's take it offline after the standup.",
        translation = "Eso es largo, hablémoslo después de la reunión.",
        commonMistake =
        "hearing 'offline' as 'sin conexión' — inside a meeting it means 'not here, " +
            "in a smaller group'.",
        confusableWith = listOf("sin conexión → offline", "fuera de la reunión → offline"),
        collocations = listOf("take it offline", "discuss it offline"),
        usagePattern = "take + [it / this] + offline",
        sourceContext = "Daily: cortar una discusión larga",
        clozeAnswer = "take it offline",
        register = RegisterPreference.Casual,
        acceptedAnswers = listOf("let us take it offline"),
    ),
    CollocationSpec(
        expression = "pick up a ticket",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "pɪk ʌp ə ˈtɪkɪt",
        definitionEn = "to take an unassigned task from the board and start working on it.",
        meaningEs = "agarrar una tarea del tablero",
        whyUseful = "it claims work without sounding like somebody had to assign it to you.",
        example = "I picked up the ticket for the crash on startup.",
        translation = "Agarré la tarea del crash al abrir la app.",
        commonMistake =
        "saying 'I took the ticket' for 'agarré la tarea' — 'take' sounds like you removed it " +
            "from the board; the verb is 'pick up'.",
        confusableWith = listOf("agarrar una tarea → pick up a ticket"),
        collocations = listOf("pick up a ticket", "pick up the next task"),
        usagePattern = "pick up + [a ticket / a task]",
        sourceContext = "Daily: decir qué tarea tomaste",
        clozeAnswer = "picked up",
        register = RegisterPreference.Casual,
    ),
    CollocationSpec(
        expression = "it's almost done",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ɪts ˈɔːlmoʊst dʌn",
        definitionEn = "the work is nearly finished but not merged yet.",
        meaningEs = "ya casi está",
        whyUseful = "the literal Spanish version puts the verb on you instead of on the work.",
        example = "It's almost done, I just need to write the tests.",
        translation = "Ya casi está, solo me faltan las pruebas.",
        commonMistake =
        "saying 'I have it almost' for 'ya casi lo tengo' — English puts it on the work: " +
            "'it's almost done'.",
        confusableWith = listOf("ya casi está → it's almost done"),
        collocations = listOf("it's almost done", "almost ready"),
        usagePattern = "it's almost + [done / ready]",
        sourceContext = "Daily: decir que estás por terminar",
        clozeAnswer = "almost done",
        acceptedAnswers = listOf("it is almost done"),
    ),
    CollocationSpec(
        expression = "need a hand with",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "niːd ə hænd wɪð",
        definitionEn = "to ask a teammate for help on one specific thing.",
        meaningEs = "necesito ayuda con",
        whyUseful = "it asks for help without sounding like you gave up on the task.",
        example = "I need a hand with the Gradle config.",
        translation = "Necesito ayuda con la configuración de Gradle.",
        commonMistake =
        "saying 'I need help in the config' for 'ayuda en' — both 'help' and 'a hand' " +
            "take 'with', never 'in'.",
        confusableWith = listOf("necesito ayuda con → need a hand with"),
        collocations = listOf("need a hand with", "give someone a hand"),
        usagePattern = "need a hand with + [noun]",
        sourceContext = "Daily: pedir ayuda",
        clozeAnswer = "a hand",
        register = RegisterPreference.Casual,
    ),
    CollocationSpec(
        expression = "follow up on",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈfɑːloʊ ʌp ɑːn",
        definitionEn = "to check again later on something that is still open.",
        meaningEs = "dar seguimiento a",
        whyUseful = "it promises a next step, and 'seguimiento' has no direct noun in the verb.",
        example = "I'll follow up on the design review this afternoon.",
        translation = "Voy a dar seguimiento a la revisión de diseño esta tarde.",
        commonMistake =
        "saying 'I'll make a following of it' for 'dar seguimiento' — the verb is " +
            "'follow up on' plus the thing.",
        confusableWith = listOf("dar seguimiento a → follow up on", "seguimiento → a follow-up"),
        collocations = listOf("follow up on a ticket", "send a follow-up"),
        usagePattern = "follow up on + [noun]",
        sourceContext = "Daily: prometer un siguiente paso",
        clozeAnswer = "follow up on",
    ),
    CollocationSpec(
        expression = "let's park that",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "lets pɑːrk ðæt",
        definitionEn = "to set a topic aside on purpose and come back to it later.",
        meaningEs = "dejemos eso para después",
        whyUseful = "it postpones a topic without dismissing the person who raised it.",
        example = "Let's park that until the design is signed off.",
        translation = "Dejemos eso para cuando el diseño esté aprobado.",
        commonMistake =
        "saying 'let's leave that' for 'dejemos eso' — 'leave it' sounds like dropping it; " +
            "'park it' means you are coming back.",
        confusableWith = listOf("dejarlo para después → park it", "abandonarlo → drop it"),
        collocations = listOf("park that for now", "park the discussion"),
        usagePattern = "park + [that / it / the topic]",
        sourceContext = "Daily: posponer un tema",
        clozeAnswer = "park that",
        register = RegisterPreference.Casual,
        acceptedAnswers = listOf("let us park that"),
    ),
    CollocationSpec(
        expression = "I have a question",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "aɪ hæv ə ˈkwestʃən",
        definitionEn = "the normal way to open a question in a meeting.",
        meaningEs = "tengo una pregunta",
        whyUseful = "'tengo una duda' becomes the one calque every native speaker notices.",
        example = "I have a question about the new endpoint.",
        translation = "Tengo una pregunta sobre el endpoint nuevo.",
        commonMistake =
        "saying 'I have a doubt' for 'tengo una duda' — a 'doubt' is mistrust in English; " +
            "you 'have a question'.",
        confusableWith = listOf("tengo una duda → I have a question", "duda (desconfianza) → doubt"),
        collocations = listOf("a question about", "a quick question"),
        usagePattern = "have a question about + [noun]",
        sourceContext = "Daily: preguntar algo en la reunión",
        clozeAnswer = "a question",
    ),
    CollocationSpec(
        expression = "get to it",
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ɡet tuː ɪt",
        definitionEn = "to reach a task and start it once you finish what you are on.",
        meaningEs = "ponerme con eso",
        whyUseful = "it commits to a time without pretending the task is already started.",
        example = "I'll get to it this afternoon, right after the release.",
        translation = "Me pongo con eso esta tarde, justo después del release.",
        commonMistake =
        "saying 'I'll arrive to it this afternoon' for 'llegar a eso' — the phrase is " +
            "'get to it' plus the time.",
        confusableWith = listOf("llegar a hacerlo → get to it"),
        collocations = listOf("get to it later", "get to the rest tomorrow"),
        usagePattern = "get to + [it / the task] + [time]",
        sourceContext = "Daily: decir cuándo vas a empezar algo",
        clozeAnswer = "get to it",
        register = RegisterPreference.Casual,
    ),
)

internal val dailyStandupPhrases: List<GeneratedLearningNote> =
    dailyStandupPhraseSpecs.map { spec -> spec.toNote(DAILY_STANDUP_DECK_ID) }
