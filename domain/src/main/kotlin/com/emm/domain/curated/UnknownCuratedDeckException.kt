package com.emm.domain.curated

class UnknownCuratedDeckException(
    val curatedDeckId: String,
) : RuntimeException("Unknown curated deck: $curatedDeckId")
