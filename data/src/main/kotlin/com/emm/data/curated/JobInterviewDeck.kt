package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.generation.LevelBand

internal const val JOB_INTERVIEW_DECK_ID = "job-interview"

object JobInterviewDeck {

    val deck: CuratedDeck = CuratedDeck(
        id = JOB_INTERVIEW_DECK_ID,
        name = "Entrevista de trabajo",
        description = "Lo que se dice en una entrevista en inglés y los calcos que te delatan.",
        tags = listOf("trabajo", "entrevista"),
        levelBand = LevelBand.B1_B2,
        notes = jobInterviewWords + jobInterviewPhrases + jobInterviewPatterns,
    )
}
