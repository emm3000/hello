package com.emm.data.flashcard

import com.emm.domain.flashcard.Flashcard

fun FlashcardEntity.toDomain() = Flashcard(
    id = id,
    word = word,
    meaning = meaning,
    translation = translation.orEmpty(),
    examples = emptyList(),
    phonetic = phonetic.orEmpty(),
)

fun List<FlashcardEntity>.toDomain() = map(FlashcardEntity::toDomain)