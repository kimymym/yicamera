package com.github.kimymym.yicamera.data.model

enum class ShootMode(val modeParam: String, val displayName: String) {
    PHOTO("capture", "사진"),
    VIDEO("record", "동영상")
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING_WIFI,
    CONNECTING_SOCKET,
    AUTHENTICATED,
    ERROR
}

data class BatteryInfo(
    val percentage: Int = 100,
    val isCharging: Boolean = false
)

data class StorageInfo(
    val totalMb: Long = 0,
    val freeMb: Long = 0,
    val status: String = "확인중",
    val errorMessage: String? = null
) {
    val freeGbFormatted: String
        get() = String.format("%.1f GB", freeMb / 1024.0)

    val totalGbFormatted: String
        get() = String.format("%.1f GB", totalMb / 1024.0)

    val usedMb: Long
        get() = if (totalMb > freeMb) totalMb - freeMb else 0

    val usageRatio: Float
        get() = if (totalMb > 0) (usedMb.toFloat() / totalMb.toFloat()).coerceIn(0f, 1f) else 0f

    val summary: String
        get() = when {
            errorMessage != null -> errorMessage
            totalMb > 0 -> "$freeGbFormatted 남음 (전체 $totalGbFormatted)"
            freeMb > 0 -> "$freeGbFormatted 남음"
            else -> "SD 카드 확인중"
        }
}

data class CameraSettings(
    val videoResolution: String = "1920x1080 60P 16:9",
    val videoResolutionOptions: List<String> = listOf(
        "1920x1080 60P 16:9",
        "1920x1080 30P 16:9",
        "1280x720 120P 16:9",
        "1280x720 60P 16:9"
    ),
    val photoResolution: String = "16M (4608x3456 4:3)",
    val photoResolutionOptions: List<String> = listOf(
        "16M (4608x3456 4:3)",
        "13M (4128x3096 4:3)",
        "11M (4608x2592 16:9)",
        "8M (3264x2448 4:3)"
    ),
    val buzzerVolume: String = "high", // high, low, mute
    val buzzerVolumeOptions: List<String> = listOf("high", "low", "mute"),
    val ledMode: String = "all",       // all, front, off
    val ledModeOptions: List<String> = listOf("all", "front", "off"),
    val autoPowerOff: String = "5m",    // off, 3m, 5m, 10m
    val autoPowerOffOptions: List<String> = listOf("off", "3m", "5m", "10m"),
    val videoRotate: String = "off",   // off, on (영상 상하 반전)
    val videoRotateOptions: List<String> = listOf("off", "on"),
    val wifiSsid: String = "YDXJ_Camera",
    val wifiPassword: String = "1234567890"
) {
    val isVideoRotateOn: Boolean get() = videoRotate.equals("on", ignoreCase = true)
}

data class CameraDeviceState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val errorMessage: String? = null,
    val token: Int = 0,
    val shootMode: ShootMode = ShootMode.PHOTO,
    val isRecording: Boolean = false,
    val recordingDurationSec: Long = 0,
    val isViewfinderActive: Boolean = false,
    val isFormattingSdCard: Boolean = false,
    val formatResult: String? = null,
    val battery: BatteryInfo = BatteryInfo(),
    val storage: StorageInfo = StorageInfo(),
    val settings: CameraSettings = CameraSettings()
)
