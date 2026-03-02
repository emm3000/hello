package com.emm.domain.flashcard

enum class TypeView {
    WordOrPhase {
        override val other: TypeView
            get() = WithCategories
    },
    WithCategories {
        override val other: TypeView
            get() = WordOrPhase
    };

    abstract val other: TypeView
}
