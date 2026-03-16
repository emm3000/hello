package com.emm.domain.sync

class ListLinkedDevicesUseCase(
    private val repository: PairingRepository,
) {
    suspend fun execute(): List<LinkedDevice> {
        return repository.listLinkedDevices()
    }
}
