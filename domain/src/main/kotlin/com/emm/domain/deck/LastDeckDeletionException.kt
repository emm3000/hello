package com.emm.domain.deck

import com.emm.domain.ids.DeckId

class LastDeckDeletionException(
    val deckId: DeckId,
) : IllegalStateException("Cannot delete the last live deck: ${deckId.value}")
