package com.emm.hello.newfeatures.store

import com.emm.domain.generation.LevelBand

data class StoreDeckItem(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val levelBand: LevelBand,
    val cardsCount: Int,
    val installedDeckId: String?,
) {

    val isInstalled: Boolean
        get() = installedDeckId != null
}
