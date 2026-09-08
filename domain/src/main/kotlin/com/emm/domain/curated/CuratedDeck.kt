package com.emm.domain.curated

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LevelBand

data class CuratedDeck(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val levelBand: LevelBand,
    val notes: List<GeneratedLearningNote>,
)
