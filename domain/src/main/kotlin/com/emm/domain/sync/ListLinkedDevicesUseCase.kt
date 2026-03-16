package com.emm.domain.sync

class ListLinkedDevicesUseCase(
    private val repository: PairingRepository,
) {
    suspend operator fun invoke(): List<LinkedDevice> {
        return repository.listLinkedDevices()
    }
}
