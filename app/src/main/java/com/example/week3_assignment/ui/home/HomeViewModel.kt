package com.example.week3_assignment.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.week3_assignment.data.Device
import com.example.week3_assignment.data.DeviceCategory
import com.example.week3_assignment.data.DeviceRepository
import com.example.week3_assignment.data.ServiceLocator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

// ---------- UI State (UI sadece bunu okur) ----------
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val totalPowerText: String = "",
    val activeDeviceText: String = "",
    val roomFilters: List<RoomFilterUiModel> = emptyList(),
    val devices: List<DeviceUiModel> = emptyList(),
    val resultText: String = "",
    val showEmptyState: Boolean = false,
)

@Immutable
data class RoomFilterUiModel(val room: String?, val label: String, val isSelected: Boolean)

@Immutable
data class DeviceUiModel(
    val id: Int,
    val name: String,
    val icon: String,
    val subtitle: String,
    val statusText: String,
    val isOnline: Boolean,
    val isActive: Boolean,
    val actionText: String,
    val isToggleEnabled: Boolean,
    val isPending: Boolean,
)

// ---------- ViewModel ----------
@OptIn(FlowPreview::class)
class HomeViewModel(
    private val repository: DeviceRepository,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val selectedRoom = MutableStateFlow<String?>(null)
    private val pendingDeviceIds = MutableStateFlow<Set<Int>>(emptySet())

    // Her tuşta yeniden hesaplamamak için debounce; boş metin anında uygulanır
    private val debouncedQuery = _searchQuery.debounce { if (it.isBlank()) 0L else 250L }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.devices, debouncedQuery, selectedRoom, pendingDeviceIds,
    ) { devices, query, room, pending ->
        buildHomeUiState(devices, query, room, pending)
    }
        .flowOn(computeDispatcher) // filtre + toplama arka planda (Default thread)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onRoomSelected(room: String?) {
        selectedRoom.update { current -> if (current == room) null else room }
    }

    fun onToggleDevice(deviceId: Int) {
        if (deviceId in pendingDeviceIds.value) return // çift tıklamayı engelle
        pendingDeviceIds.update { it + deviceId }
        viewModelScope.launch {
            try {
                repository.toggleDevice(deviceId)
            } finally {
                pendingDeviceIds.update { it - deviceId }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(ServiceLocator.deviceRepository) }
        }
    }
}

// ---------- Tüm filtreleme ve hesaplama burada (UI'da değil) ----------
private val TurkishLocale: Locale = Locale.forLanguageTag("tr-TR")

internal fun buildHomeUiState(
    devices: List<Device>,
    query: String,
    selectedRoom: String?,
    pendingIds: Set<Int>,
): HomeUiState {
    val consuming = devices.filter { it.isOnline && it.isActive }
    val totalWatt = consuming.sumOf { it.powerWatt }

    val normalizedQuery = query.trim().lowercase(TurkishLocale)
    val visible = devices.filter { d ->
        (selectedRoom == null || d.room == selectedRoom) &&
                (normalizedQuery.isEmpty() || d.name.lowercase(TurkishLocale).contains(normalizedQuery))
    }

    val roomFilters = buildList {
        add(RoomFilterUiModel(null, "Tümü", selectedRoom == null))
        devices.map { it.room }.distinct().forEach { add(RoomFilterUiModel(it, it, it == selectedRoom)) }
    }

    return HomeUiState(
        isLoading = false,
        totalPowerText = formatWatt(totalWatt),
        activeDeviceText = "${consuming.size} cihaz şu an enerji tüketiyor",
        roomFilters = roomFilters,
        devices = visible.map { it.toUiModel(it.id in pendingIds) },
        resultText = "${visible.size} / ${devices.size} cihaz listeleniyor",
        showEmptyState = visible.isEmpty(),
    )
}

private fun formatWatt(watt: Double): String = String.format(TurkishLocale, "%.1f W", watt)

private fun Device.toUiModel(isPending: Boolean) = DeviceUiModel(
    id = id,
    name = name,
    icon = when (category) {
        DeviceCategory.LIGHT -> "💡"
        DeviceCategory.CAMERA -> "📷"
        DeviceCategory.THERMOSTAT -> "🌡️"
        DeviceCategory.VACUUM -> "🧹"
    },
    subtitle = "$room • " + when (category) {
        DeviceCategory.LIGHT -> "Aydınlatma"
        DeviceCategory.CAMERA -> "Kamera"
        DeviceCategory.THERMOSTAT -> "İklimlendirme"
        DeviceCategory.VACUUM -> "Süpürge"
    },
    statusText = when {
        !isOnline -> "Çevrimdışı"
        isActive -> "Açık • ${formatWatt(powerWatt)}"
        else -> "Kapalı • ${formatWatt(powerWatt)}"
    },
    isOnline = isOnline,
    isActive = isActive,
    actionText = if (isActive) "Kapat" else "Aç",
    isToggleEnabled = isOnline && !isPending,
    isPending = isPending,
)