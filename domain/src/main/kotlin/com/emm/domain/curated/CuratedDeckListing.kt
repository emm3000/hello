package com.emm.domain.curated

import com.emm.domain.ids.DeckId

data class CuratedDeckListing(
    val deck: CuratedDeck,
    val installedDeckId: DeckId?,
) {

    val isInstalled: Boolean
        get() = installedDeckId != null
}
