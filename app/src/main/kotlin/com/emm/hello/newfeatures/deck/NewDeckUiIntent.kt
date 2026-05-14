package com.emm.hello.newfeatures.deck

import com.emm.hello.core.mvi.MviIntent

sealed interface NewDeckUiIntent : MviIntent {

    data class NameChanged(val name: String) : NewDeckUiIntent
    data class DescriptionChanged(val description: String) : NewDeckUiIntent
    data class TagsChanged(val tags: List<String>) : NewDeckUiIntent
    data object Submit : NewDeckUiIntent
}
