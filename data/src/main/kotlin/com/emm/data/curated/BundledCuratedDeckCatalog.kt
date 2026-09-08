package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.curated.CuratedDeckCatalog

class BundledCuratedDeckCatalog : CuratedDeckCatalog {

    override fun decks(): List<CuratedDeck> =
        listOf(
            SpanishTrapsDeck.deck,
            JobInterviewDeck.deck,
            TechInterviewDeck.deck,
            DailyStandupDeck.deck,
        )
}
