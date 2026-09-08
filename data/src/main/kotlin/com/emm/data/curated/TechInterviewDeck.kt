package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.generation.LevelBand

internal const val TECH_INTERVIEW_DECK_ID = "tech-interview"

object TechInterviewDeck {

    val deck: CuratedDeck = CuratedDeck(
        id = TECH_INTERVIEW_DECK_ID,
        name = "Entrevista técnica",
        description = "El inglés de la entrevista técnica de software y los calcos que te delatan.",
        tags = listOf("trabajo", "entrevista", "software"),
        levelBand = LevelBand.B1_B2,
        notes = techInterviewWords + techInterviewPhrases + techInterviewPatterns,
    )
}
