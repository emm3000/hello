package com.emm.hello.newfeatures.deck

sealed interface NewDeckAction {

    data class NameChanged(val name: String) : NewDeckAction
    data class DescriptionChanged(val description: String) : NewDeckAction
    data object Submit : NewDeckAction
}
