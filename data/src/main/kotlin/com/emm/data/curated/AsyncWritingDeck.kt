package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.generation.LevelBand

internal const val ASYNC_WRITING_DECK_ID = "async-writing"

object AsyncWritingDeck {

    val deck: CuratedDeck = CuratedDeck(
        id = ASYNC_WRITING_DECK_ID,
        name = "Slack y pull requests",
        description = "Lo que escribes en Slack, PRs y tickets, y los calcos que te delatan.",
        tags = listOf("trabajo", "escrito", "software"),
        levelBand = LevelBand.B1_B2,
        notes = asyncWritingWords + asyncWritingPhrases + asyncWritingPatterns,
    )
}
