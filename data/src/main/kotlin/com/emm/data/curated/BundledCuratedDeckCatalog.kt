package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.curated.CuratedDeckCatalog

class BundledCuratedDeckCatalog : CuratedDeckCatalog {

    override fun decks(): List<CuratedDeck> =
        listOf(
            FirstCallsDeck.deck,
            DailyStandupDeck.deck,
            AsyncWritingDeck.deck,
            JobInterviewDeck.deck,
            TechInterviewDeck.deck,
            SpanishTrapsDeck.deck,
        )
}
