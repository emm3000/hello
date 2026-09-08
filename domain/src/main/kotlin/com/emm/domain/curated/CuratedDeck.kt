package com.emm.domain.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LevelBand
import com.emm.domain.ids.DeckId

data class CuratedDeck(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val levelBand: LevelBand,
    val notes: List<GeneratedLearningNote>,
) {

    val installedDeckId: DeckId
        get() = DeckId.from("curated-$id")
}
