package com.example.week3_assignment.data

enum class DeviceCategory { LIGHT, CAMERA, THERMOSTAT, VACUUM }

data class Device(
    val id: Int,
    val name: String,
    val category: DeviceCategory,
    val room: String,
    val isOnline: Boolean,
    val isActive: Boolean,
    val powerWatt: Double,
)

object SampleDevices {
    val all: List<Device> = listOf(
        Device(1, "Köşe Lambası", DeviceCategory.LIGHT, "Salon", isOnline = true, isActive = true, powerWatt = 15.0),
        Device(2, "Tavan Avizesi", DeviceCategory.LIGHT, "Salon", isOnline = true, isActive = false, powerWatt = 60.0),
        Device(3, "Bahçe Kamerası", DeviceCategory.CAMERA, "Bahçe", isOnline = true, isActive = true, powerWatt = 8.5),
        Device(4, "Giriş Kamerası", DeviceCategory.CAMERA, "Antre", isOnline = true, isActive = true, powerWatt = 8.5),
        Device(5, "Merkezi Termostat", DeviceCategory.THERMOSTAT, "Salon", isOnline = true, isActive = true, powerWatt = 12.0),
        Device(6, "Yatak Odası Kliması", DeviceCategory.THERMOSTAT, "Yatak Odası", isOnline = true, isActive = false, powerWatt = 1200.0),
        Device(7, "Mutfak Spotları", DeviceCategory.LIGHT, "Mutfak", isOnline = true, isActive = false, powerWatt = 45.0),
        Device(8, "Roborock S7", DeviceCategory.VACUUM, "Koridor", isOnline = true, isActive = true, powerWatt = 50.0),
        Device(9, "Balkon Aydınlatması", DeviceCategory.LIGHT, "Balkon", isOnline = false, isActive = false, powerWatt = 25.0),
        Device(10, "Çalışma Odası Lambası", DeviceCategory.LIGHT, "Ofis", isOnline = true, isActive = true, powerWatt = 12.0),
    )
}