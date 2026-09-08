package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.generation.LevelBand

internal const val DAILY_STANDUP_DECK_ID = "daily-standup"

object DailyStandupDeck {

    val deck: CuratedDeck = CuratedDeck(
        id = DAILY_STANDUP_DECK_ID,
        name = "Reunión daily",
        description = "El inglés de la reunión diaria del equipo y los calcos que te delatan.",
        tags = listOf("trabajo", "reuniones", "software"),
        levelBand = LevelBand.B1_B2,
        notes = dailyStandupWords + dailyStandupPhrases + dailyStandupPatterns,
    )
}
