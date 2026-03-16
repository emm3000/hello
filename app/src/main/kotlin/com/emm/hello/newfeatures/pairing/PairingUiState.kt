package com.emm.hello.newfeatures.pairing

import com.emm.domain.sync.LinkedDevice

data class PairingUiState(
    val isLoading: Boolean = false,
    val isSubmittingJoin: Boolean = false,
    val isGeneratingCode: Boolean = false,
    val generatedCode: String? = null,
    val generatedCodeExpiresAt: String? = null,
    val joinCode: String = "",
    val devices: List<LinkedDevice> = emptyList(),
    val error: String? = null,
    val success: String? = null,
)
