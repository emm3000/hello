package com.emm.hello.newfeatures.deck

sealed interface NewDeckUiIntent {

    data class NameChanged(val name: String) : NewDeckUiIntent
    data class DescriptionChanged(val description: String) : NewDeckUiIntent
    data object Submit : NewDeckUiIntent
}
