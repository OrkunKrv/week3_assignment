package com.example.week3_assignment.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers

/** ViewModel'e bağlanan tek yer: state'i toplar, event'leri iletir. */
@Composable
fun HomeRoute(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    // Main.immediate: TextField senkron güncellensin (imleç atlamasın)
    val searchQuery by viewModel.searchQuery.collectAsState(context = Dispatchers.Main.immediate)

    HomeScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onRoomSelected = viewModel::onRoomSelected,
        onToggleDevice = viewModel::onToggleDevice,
    )
}

/** Tamamen stateless: filtreleme / hesaplama yok, sadece çizim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRoomSelected: (String?) -> Unit,
    onToggleDevice: (Int) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Akıllı Ev Kontrol Merkezi") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EnergyCard(uiState.totalPowerText, uiState.activeDeviceText)
            SearchField(searchQuery, onSearchQueryChange)
            RoomFilterRow(uiState.roomFilters, onRoomSelected)

            when {
                uiState.isLoading -> CenterBox(Modifier.weight(1f)) { CircularProgressIndicator() }
                uiState.showEmptyState -> CenterBox(Modifier.weight(1f)) { Text("Eşleşen cihaz bulunamadı") }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "header") {
                        Text(
                            uiState.resultText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(uiState.devices, key = { it.id }, contentType = { "device" }) { device ->
                        DeviceCard(device, onToggleDevice, Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun EnergyCard(totalPowerText: String, activeDeviceText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("⚡ Anlık Enerji Tüketimi", style = MaterialTheme.typography.titleSmall)
            AnimatedContent(targetState = totalPowerText, label = "totalPower") { text ->
                Text(text, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            Text(activeDeviceText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Cihaz ara…") },
        leadingIcon = { Text("🔍") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Text("✕") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomFilterRow(filters: List<RoomFilterUiModel>, onRoomSelected: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters, key = { it.label }) { filter ->
            FilterChip(
                selected = filter.isSelected,
                onClick = { onRoomSelected(filter.room) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceUiModel, onToggleDevice: (Int) -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (device.isOnline) 1f else 0.55f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (device.isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center,
            ) { Text(device.icon, fontSize = 20.sp) }

            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    device.statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        !device.isOnline -> MaterialTheme.colorScheme.error
                        device.isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            val onClick = { onToggleDevice(device.id) }
            val btnModifier = Modifier.widthIn(min = 84.dp)
            if (device.isActive) {
                Button(onClick = onClick, enabled = device.isToggleEnabled, modifier = btnModifier) { ButtonContent(device) }
            } else {
                OutlinedButton(onClick = onClick, enabled = device.isToggleEnabled, modifier = btnModifier) { ButtonContent(device) }
            }
        }
    }
}

@Composable
private fun ButtonContent(device: DeviceUiModel) {
    if (device.isPending) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
        Text(device.actionText)
    }
}

@Composable
private fun CenterBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
}