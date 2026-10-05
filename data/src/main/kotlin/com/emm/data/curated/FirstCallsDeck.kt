package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.generation.LevelBand

internal const val FIRST_CALLS_DECK_ID = "first-calls"

object FirstCallsDeck {

    val deck: CuratedDeck = CuratedDeck(
        id = FIRST_CALLS_DECK_ID,
        name = "Primeras llamadas",
        description = "Lo mínimo para entender, pedir que repitan y responder en una llamada de trabajo.",
        tags = listOf("trabajo", "básico", "llamadas"),
        levelBand = LevelBand.A1_A2,
        notes = firstCallsWords + firstCallsPhrases + firstCallsPatterns,
    )
}
