package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.generation.LevelBand

internal const val SPANISH_TRAPS_DECK_ID = "spanish-traps"

object SpanishTrapsDeck {

    val deck: CuratedDeck = CuratedDeck(
        id = SPANISH_TRAPS_DECK_ID,
        name = "Trampas del español",
        description = "Falsos amigos, preposiciones y calcos que delatan a un hispanohablante.",
        tags = listOf("falsos amigos", "errores comunes"),
        levelBand = LevelBand.B1_B2,
        notes = spanishTrapsFalseFriends + spanishTrapsCollocations + spanishTrapsPatterns,
    )
}
