package com.emm.data.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag

private val dailyStandupWordSpecs: List<WordSpec> = listOf(
    WordSpec(
        expression = "standup",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈstændʌp",
        definitionEn = "the short daily meeting where the team syncs on progress.",
        meaningEs = "la reunión diaria del equipo",
        whyUseful = "spanish teams say 'el daily', and in English that word cannot stand alone as a noun.",
        example = "I'll bring it up at the standup tomorrow.",
        translation = "Lo comento en la reunión de mañana.",
        commonMistake =
        "saying 'in the daily' for 'en el daily' — English needs a noun after it: " +
            "'at the standup' or 'at the daily standup'.",
        confusableWith = listOf("el daily → the standup", "reunión diaria → daily standup"),
        sourceContext = "Daily: nombrar la reunión",
        collocations = listOf("at the standup", "a daily standup"),
    ),
    WordSpec(
        expression = "blocker",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈblɑːkər",
        definitionEn = "the one thing that stops a task from moving forward.",
        meaningEs = "lo que te impide avanzar",
        whyUseful = "it is the exact noun the standup asks you for, and the Spanish word pulls a legal one.",
        example = "My only blocker is the missing API key.",
        translation = "Lo único que me frena es la clave de la API que falta.",
        commonMistake =
        "saying 'I have an impediment' for 'tengo un impedimento' — that word belongs to law; " +
            "in a team the noun is 'a blocker'.",
        confusableWith = listOf("impedimento → blocker", "obstáculo → blocker"),
        sourceContext = "Daily: nombrar lo que te frena",
        collocations = listOf("my only blocker", "raise a blocker"),
    ),
    WordSpec(
        expression = "update",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈʌpdeɪt",
        definitionEn = "a short report of what has changed since you last spoke.",
        meaningEs = "novedad / avance",
        whyUseful = "it is what your turn is called, and 'actualización' invites a word that does not exist.",
        example = "I have a quick update on the login screen.",
        translation = "Tengo una novedad rápida sobre la pantalla de login.",
        commonMistake =
        "saying 'I have an actualization' for 'una actualización' — 'actualization' is not used here; " +
            "the noun is 'an update'.",
        confusableWith = listOf("actualización (novedad) → update", "novedades → updates"),
        sourceContext = "Daily: dar tu avance del día",
        collocations = listOf("a quick update", "give an update"),
    ),
    WordSpec(
        expression = "currently",
        partOfSpeech = PartOfSpeechTag.Adverb,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈkɜːrəntli",
        definitionEn = "at this moment, describing the work that is happening right now.",
        meaningEs = "actualmente / ahora mismo",
        whyUseful = "the lookalike adverb means something completely different and changes your sentence.",
        example = "I'm currently working on the sync bug.",
        translation = "Ahora mismo estoy trabajando en el bug de sincronización.",
        commonMistake =
        "saying 'actually I'm working on it' for 'actualmente' — 'actually' means 'en realidad'; " +
            "'ahora mismo' is 'currently'.",
        confusableWith = listOf("actualmente → currently", "en realidad → actually"),
        sourceContext = "Daily: decir en qué estás trabajando ahora",
        collocations = listOf("currently working on", "currently blocked"),
    ),
    WordSpec(
        expression = "attend",
        partOfSpeech = PartOfSpeechTag.Verb,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "əˈtend",
        definitionEn = "to be present at a meeting.",
        meaningEs = "asistir (a una reunión)",
        whyUseful = "the false friend turns 'no puedo asistir' into an offer of help.",
        example = "I can't attend the standup tomorrow.",
        translation = "No puedo asistir a la reunión de mañana.",
        commonMistake =
        "saying 'I will assist to the meeting' for 'asistir' — 'assist' means 'ayudar', " +
            "and 'attend' takes no preposition.",
        confusableWith = listOf("asistir (a una reunión) → attend", "asistir (ayudar) → assist"),
        sourceContext = "Daily: avisar que no vas a estar",
        collocations = listOf("attend the standup", "attend a meeting"),
    ),
    WordSpec(
        expression = "outstanding",
        partOfSpeech = PartOfSpeechTag.Adjective,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "aʊtˈstændɪŋ",
        definitionEn = "still not finished or not answered after some time has passed.",
        meaningEs = "pendiente (sin resolver)",
        whyUseful = "in English the task is pending, never the person waiting for it.",
        example = "The only outstanding item is the QA sign-off.",
        translation = "Lo único pendiente es la aprobación de QA.",
        commonMistake =
        "saying 'I am pending of the review' for 'estoy pendiente de la revisión' — a person is not " +
            "pending; the review is 'outstanding'.",
        confusableWith = listOf("pendiente (sin resolver) → outstanding", "estar pendiente de → be waiting on"),
        sourceContext = "Daily: decir qué quedó sin cerrar",
        collocations = listOf("an outstanding item", "still outstanding"),
    ),
    WordSpec(
        expression = "heads-up",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˌhedz ˈʌp",
        definitionEn = "a short warning given in advance so nobody is surprised later.",
        meaningEs = "aviso anticipado",
        whyUseful = "it is how a team warns each other, and the Spanish verb pulls the advertising word.",
        example = "Just a heads-up: the staging server is down.",
        translation = "Solo un aviso: el servidor de staging está caído.",
        commonMistake =
        "saying 'I want to advertise you' for 'quiero avisarte' — 'advertise' is paid publicity; " +
            "a short warning is 'a heads-up'.",
        confusableWith = listOf("aviso previo → a heads-up", "avisar (advertir) → warn"),
        sourceContext = "Daily: avisar de algo antes de que pase",
        collocations = listOf("give someone a heads-up", "just a heads-up"),
    ),
    WordSpec(
        expression = "estimate",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "ˈestɪmət",
        definitionEn = "your best guess of how long a piece of work will take.",
        meaningEs = "estimación",
        whyUseful = "you give one almost every day, and the Spanish adjective invents an English noun.",
        example = "My estimate is two more days for the migration.",
        translation = "Mi estimación es de dos días más para la migración.",
        commonMistake =
        "saying 'my estimative' for 'mi estimativo' — 'estimative' is not a noun in English; " +
            "the noun is 'an estimate'.",
        confusableWith = listOf("estimación → estimate", "estimado (adjetivo) → estimated"),
        sourceContext = "Daily: dar un plazo para tu tarea",
        collocations = listOf("give an estimate", "a rough estimate"),
    ),
    WordSpec(
        expression = "scope",
        partOfSpeech = PartOfSpeechTag.Noun,
        levelBand = LevelBand.B1_B2,
        domain = LearningDomain.Work,
        ipa = "skoʊp",
        definitionEn = "the work a task actually includes, and nothing beyond it.",
        meaningEs = "alcance del trabajo",
        whyUseful = "it is how you defend a small task, and the fixed phrase drops the article.",
        example = "Search filters are out of scope for this ticket.",
        translation = "Los filtros de búsqueda están fuera del alcance de esta tarea.",
        commonMistake =
        "saying 'that is out of the scope' for 'fuera del alcance' — the fixed phrase carries " +
            "no article: 'out of scope'.",
        confusableWith = listOf("alcance del trabajo → scope", "fuera del alcance → out of scope"),
        sourceContext = "Daily: acotar lo que vas a hacer",
        collocations = listOf("out of scope", "the scope of the ticket"),
    ),
)

internal val dailyStandupWords: List<GeneratedLearningNote> =
    dailyStandupWordSpecs.map { spec -> spec.toNote(DAILY_STANDUP_DECK_ID) }
