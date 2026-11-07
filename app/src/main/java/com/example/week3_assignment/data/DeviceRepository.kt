package com.example.week3_assignment.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Tek doğruluk kaynağı (single source of truth). */
interface DeviceRepository {
    val devices: StateFlow<List<Device>>
    suspend fun toggleDevice(deviceId: Int)
}

/** IoT ağ isteğini taklit eder; işlem IO thread'inde yapılır, Main thread bloklanmaz. */
class FakeDeviceRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val networkDelayMillis: Long = 300L,
) : DeviceRepository {

    private val _devices = MutableStateFlow(SampleDevices.all)
    override val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    override suspend fun toggleDevice(deviceId: Int) {
        withContext(ioDispatcher) {
            delay(networkDelayMillis)
            _devices.update { list ->
                list.map { if (it.id == deviceId && it.isOnline) it.copy(isActive = !it.isActive) else it }
            }
        }
    }
}

/** Basit manuel DI: uygulama boyunca tek repository. */
object ServiceLocator {
    val deviceRepository: DeviceRepository by lazy { FakeDeviceRepository() }
}