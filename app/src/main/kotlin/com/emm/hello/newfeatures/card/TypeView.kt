package com.emm.hello.newfeatures.card

enum class TypeView {
    WordOrPhase {
        override val other: TypeView
            get() = WithCategories
    },
    WithCategories {
        override val other: TypeView
            get() = WordOrPhase
    },
    WithAiHelp {
        override val other: TypeView
            get() = WordOrPhase
    };

    abstract val other: TypeView
}
