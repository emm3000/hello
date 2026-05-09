package com.emm.hello.newfeatures.deck

data class NewDeckUiState(
    val name: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false,
) {

    val isValid: Boolean
        get() = name.isNotBlank()
}
