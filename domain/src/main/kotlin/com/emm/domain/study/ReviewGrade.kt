package com.emm.domain.study

enum class ReviewGrade { AGAIN, HARD, GOOD, EASY }

// FSRS-6 rating convention: AGAIN=1, HARD=2, GOOD=3, EASY=4.
fun ReviewGrade.toFsrsRating(): Int = ordinal + 1
