package com.emm.domain.study

enum class ReviewGrade { AGAIN, HARD, GOOD, EASY }

/**
 * FSRS-6 grade integer convention: AGAIN=1, HARD=2, GOOD=3, EASY=4.
 * Used to persist the real review grade (replacing the legacy hardcoded "review" literal).
 */
fun ReviewGrade.toFsrsRating(): Int = ordinal + 1
