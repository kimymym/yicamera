package com.github.kimymym.yicamera.data.repository

import android.content.Context
import android.util.Log
import com.github.kimymym.yicamera.data.model.BatteryInfo
import com.github.kimymym.yicamera.data.model.CameraDeviceState
import com.github.kimymym.yicamera.data.model.CameraMediaItem
import com.github.kimymym.yicamera.data.model.CameraShootException
import com.github.kimymym.yicamera.data.model.ConnectionStatus
import com.github.kimymym.yicamera.data.model.MediaType
import com.github.kimymym.yicamera.data.model.ShootMode
import com.github.kimymym.yicamera.data.model.StorageInfo
import com.github.kimymym.yicamera.data.model.YiCommand
import com.github.kimymym.yicamera.data.model.YiMsgIds
import com.github.kimymym.yicamera.data.network.NetworkManager
import com.github.kimymym.yicamera.data.socket.YiSocketClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

interface YiCameraRepository {
    val cameraState: StateFlow<CameraDeviceState>
    val isWifiConnected: StateFlow<Boolean>

    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun startViewfinder(): Result<Unit>
    suspend fun stopViewfinder(): Result<Unit>
    suspend fun switchMode(mode: ShootMode): Result<Unit>
    suspend fun takePhoto(): Result<Unit>
    suspend fun startRecording(): Result<Unit>
    suspend fun stopRecording(): Result<Unit>
    suspend fun fetchAllSettings(): Result<Unit>
    suspend fun setSetting(type: String, value: String): Result<Unit>
    suspend fun setWifiConfig(ssid: String, password: String): Result<Unit>
    suspend fun formatSdCard(): Result<Unit>
    fun clearFormatResult()
    suspend fun refreshBatteryAndStorage()
    suspend fun fetchMediaList(): Result<List<CameraMediaItem>>
    suspend fun downloadMediaFile(item: CameraMediaItem, onProgress: (Float) -> Unit): Result<Boolean>
    fun isConnected(): Boolean
    suspend fun checkAndRestoreState(): Boolean
}

@Singleton
class DefaultYiCameraRepository @Inject constructor(
    private val socketClient: YiSocketClient,
    private val networkManager: NetworkManager,
    @ApplicationContext private val context: Context
) : YiCameraRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recordTimerJob: Job? = null
    private var statusPollingJob: Job? = null

    private val _cameraState = MutableStateFlow(CameraDeviceState())
    override val cameraState: StateFlow<CameraDeviceState> = _cameraState.asStateFlow()

    override val isWifiConnected: StateFlow<Boolean> = networkManager.isWifiConnected

    init {
        networkManager.startMonitoring()
        observeSocketResponses()
    }

    private fun observeSocketResponses() {
        repoScope.launch {
            socketClient.responseFlow.collect { resp ->
                when (resp.msgId) {
                    7 -> { // 비동기 알림 (SD 상태, 충전 등)
                        when (resp.type) {
                            "sd_card_status" -> {
                                if (resp.param?.jsonPrimitive?.content == "ready") {
                                    refreshBatteryAndStorage()
                                }
                            }
                            "sd_card_remove" -> {
                                _cameraState.update {
                                    it.copy(storage = StorageInfo(status = "제거됨", errorMessage = "SD 카드가 분리되었습니다."))
                                }
                            }
                            "sd_card_inserd" -> {
                                refreshBatteryAndStorage()
                            }
                            "sdcard_need_format" -> {
                                _cameraState.update {
                                    it.copy(storage = it.storage.copy(status = "포맷 필요", errorMessage = "SD 카드 포맷이 필요합니다."))
                                }
                            }
                            "sdcard_full" -> {
                                _cameraState.update {
                                    it.copy(storage = it.storage.copy(status = "용량 부족", errorMessage = "SD 카드 용량이 가득 찼습니다."))
                                }
                            }
                        }
                    }
                    YiMsgIds.MSG_GET_BATTERY_STATUS -> {
                        val level = resp.param?.jsonPrimitive?.content?.toIntOrNull() ?: 100
                        _cameraState.update {
                            it.copy(battery = BatteryInfo(percentage = level, isCharging = false))
                        }
                    }
                }
            }
        }
    }

    override suspend fun connect(): Result<Unit> {
        _cameraState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING_SOCKET, errorMessage = null) }

        val connResult = socketClient.connect(repoScope)
        if (connResult.isFailure) {
            _cameraState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = "소켓 연결 실패 (192.168.42.1:7878). 카메라 Wi-Fi 연결을 확인하세요."
                )
            }
            return connResult
        }

        // 세션 시작 & 토큰 수신
        val tokenResult = socketClient.startSession()
        if (tokenResult.isFailure) {
            _cameraState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = "세션 인증 실패"
                )
            }
            return Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token failed"))
        }

        val token = tokenResult.getOrThrow()
        _cameraState.update {
            it.copy(
                connectionStatus = ConnectionStatus.AUTHENTICATED,
                token = token
            )
        }

        // 초기 상태 조회 및 뷰파인더 활성화
        refreshBatteryAndStorage()
        fetchAllSettings()
        startViewfinder()
        startStatusPolling()

        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        statusPollingJob?.cancel()
        recordTimerJob?.cancel()
        stopViewfinder()
        socketClient.disconnect()
        _cameraState.update {
            CameraDeviceState(connectionStatus = ConnectionStatus.DISCONNECTED)
        }
    }

    override suspend fun startViewfinder(): Result<Unit> {
        val token = socketClient.getSessionToken()
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_START_VF,
            token = token,
            param = "none_force"
        )
        val resp = socketClient.sendCommand(cmd)
        return if (resp != null && resp.rval >= 0) {
            _cameraState.update { it.copy(isViewfinderActive = true) }
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to start VF"))
        }
    }

    override suspend fun stopViewfinder(): Result<Unit> {
        val token = socketClient.getSessionToken()
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_STOP_VF,
            token = token
        )
        socketClient.sendCommand(cmd)
        _cameraState.update { it.copy(isViewfinderActive = false) }
        return Result.success(Unit)
    }

    override suspend fun switchMode(mode: ShootMode): Result<Unit> {
        val token = socketClient.getSessionToken()
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_SET_SETTING,
            token = token,
            type = "system_mode",
            param = mode.modeParam
        )
        val resp = socketClient.sendCommand(cmd)
        return if (resp != null && resp.rval == 0) {
            _cameraState.update { it.copy(shootMode = mode) }
            Result.success(Unit)
        } else {
            Result.failure(Exception("Mode switch failed"))
        }
    }

    override suspend fun takePhoto(): Result<Unit> {
        val token = socketClient.getSessionToken()
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_TAKE_PHOTO,
            token = token
        )
        val resp = socketClient.sendCommand(cmd)
        return if (resp != null && resp.rval == 0) {
            Result.success(Unit)
        } else {
            val rval = resp?.rval ?: -999
            Log.e("YiCameraRepository", "Take photo failed: rval=$rval")
            Result.failure(CameraShootException(rval, "사진 촬영 실패 (코드: $rval)"))
        }
    }

    override suspend fun startRecording(): Result<Unit> {
        val token = socketClient.getSessionToken()
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_START_RECORD,
            token = token
        )
        val resp = socketClient.sendCommand(cmd)
        return if (resp != null && resp.rval == 0) {
            _cameraState.update { it.copy(isRecording = true, recordingDurationSec = 0) }
            startRecordTimer()
            Result.success(Unit)
        } else {
            val rval = resp?.rval ?: -999
            Log.e("YiCameraRepository", "Record start failed: rval=$rval")
            Result.failure(CameraShootException(rval, "동영상 녹화 시작 실패 (코드: $rval)"))
        }
    }

    override suspend fun stopRecording(): Result<Unit> {
        val token = socketClient.getSessionToken()
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_STOP_RECORD,
            token = token
        )
        val resp = socketClient.sendCommand(cmd)
        recordTimerJob?.cancel()
        _cameraState.update { it.copy(isRecording = false, recordingDurationSec = 0) }
        return if (resp != null && resp.rval == 0) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Record stop failed"))
        }
    }

    override suspend fun fetchAllSettings(): Result<Unit> {
        val token = socketClient.getSessionToken()
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_GET_ALL_SETTINGS,
            token = token
        )
        val resp = socketClient.sendCommand(cmd, timeoutMs = 6000)
        if (resp != null && resp.rval == 0) {
            parseAllSettings(resp.param)
        }

        // Wi-Fi SSID / 비밀번호 개별 조회 (일부 펌웨어는 ALL_SETTINGS에 미포함)
        fetchWifiConfig(token)

        return if (resp != null && resp.rval == 0) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to fetch settings from camera"))
        }
    }

    private suspend fun fetchWifiConfig(token: Int) {
        if (token == 0) return
        try {
            val ssidResp = socketClient.sendCommand(
                YiCommand(msgId = YiMsgIds.MSG_GET_SETTING, token = token, type = "wifi_ssid"),
                timeoutMs = 1500
            )
            val ssidVal = ssidResp?.param?.jsonPrimitive?.content
            if (!ssidVal.isNullOrBlank()) {
                _cameraState.update { it.copy(settings = it.settings.copy(wifiSsid = ssidVal)) }
            }
        } catch (e: Exception) {
            Log.w("YiCameraRepository", "Failed to query wifi_ssid", e)
        }

        try {
            val pwdResp = socketClient.sendCommand(
                YiCommand(msgId = YiMsgIds.MSG_GET_SETTING, token = token, type = "wifi_password"),
                timeoutMs = 1500
            )
            val pwdVal = pwdResp?.param?.jsonPrimitive?.content
            if (!pwdVal.isNullOrBlank()) {
                _cameraState.update { it.copy(settings = it.settings.copy(wifiPassword = pwdVal)) }
            }
        } catch (e: Exception) {
            Log.w("YiCameraRepository", "Failed to query wifi_password", e)
        }
    }

    private fun parseAllSettings(param: JsonElement?) {
        try {
            val array = param?.jsonArray ?: return
            var currentSettings = _cameraState.value.settings

            for (elem in array) {
                val obj = try { elem.jsonObject } catch (e: Exception) { continue }
                if (obj.containsKey("video_resolution")) {
                    val curr = obj["video_resolution"]?.jsonPrimitive?.content ?: currentSettings.videoResolution
                    val opts = obj["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: currentSettings.videoResolutionOptions
                    currentSettings = currentSettings.copy(videoResolution = curr, videoResolutionOptions = opts)
                }
                if (obj.containsKey("photo_size")) {
                    val curr = obj["photo_size"]?.jsonPrimitive?.content ?: currentSettings.photoResolution
                    val opts = obj["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: currentSettings.photoResolutionOptions
                    currentSettings = currentSettings.copy(photoResolution = curr, photoResolutionOptions = opts)
                }
                if (obj.containsKey("buzzer_volume")) {
                    val curr = obj["buzzer_volume"]?.jsonPrimitive?.content ?: currentSettings.buzzerVolume
                    val opts = obj["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: currentSettings.buzzerVolumeOptions
                    currentSettings = currentSettings.copy(buzzerVolume = curr, buzzerVolumeOptions = opts)
                }
                if (obj.containsKey("led_mode")) {
                    val curr = obj["led_mode"]?.jsonPrimitive?.content ?: currentSettings.ledMode
                    val opts = obj["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: currentSettings.ledModeOptions
                    currentSettings = currentSettings.copy(ledMode = curr, ledModeOptions = opts)
                }
                if (obj.containsKey("auto_power_off")) {
                    val curr = obj["auto_power_off"]?.jsonPrimitive?.content ?: currentSettings.autoPowerOff
                    val opts = obj["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: currentSettings.autoPowerOffOptions
                    currentSettings = currentSettings.copy(autoPowerOff = curr, autoPowerOffOptions = opts)
                }
                if (obj.containsKey("video_rotate") || obj["type"]?.jsonPrimitive?.content == "video_rotate") {
                    val curr = obj["video_rotate"]?.jsonPrimitive?.content
                        ?: obj["param"]?.jsonPrimitive?.content
                        ?: currentSettings.videoRotate
                    val opts = obj["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: currentSettings.videoRotateOptions
                    currentSettings = currentSettings.copy(videoRotate = curr, videoRotateOptions = opts)
                }
                if (obj.containsKey("wifi_ssid") || obj["type"]?.jsonPrimitive?.content == "wifi_ssid") {
                    val curr = obj["wifi_ssid"]?.jsonPrimitive?.content
                        ?: obj["param"]?.jsonPrimitive?.content
                        ?: currentSettings.wifiSsid
                    currentSettings = currentSettings.copy(wifiSsid = curr)
                }
                if (obj.containsKey("wifi_password") || obj["type"]?.jsonPrimitive?.content == "wifi_password") {
                    val curr = obj["wifi_password"]?.jsonPrimitive?.content
                        ?: obj["param"]?.jsonPrimitive?.content
                        ?: currentSettings.wifiPassword
                    currentSettings = currentSettings.copy(wifiPassword = curr)
                }
            }
            _cameraState.update { it.copy(settings = currentSettings) }
            android.util.Log.d("YiCameraRepository", "Updated settings from camera: $currentSettings")
        } catch (e: Exception) {
            android.util.Log.e("YiCameraRepository", "Error parsing camera settings", e)
        }
    }

    override suspend fun setSetting(type: String, value: String): Result<Unit> {
        val token = socketClient.getSessionToken()
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_SET_SETTING,
            token = token,
            type = type,
            param = value
        )
        val resp = socketClient.sendCommand(cmd)
        return if (resp != null && resp.rval == 0) {
            fetchAllSettings() // 최신 상태 즉시 재동기화
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed setting: $type"))
        }
    }

    override suspend fun setWifiConfig(ssid: String, password: String): Result<Unit> {
        val token = socketClient.getSessionToken()
        if (token == 0) return Result.failure(Exception("카메라와 연결되어 있지 않습니다."))

        if (password.length < 8) {
            return Result.failure(Exception("Wi-Fi 비밀번호는 최소 8자 이상이어야 합니다."))
        }

        // 1. Wi-Fi SSID 변경
        val ssidCmd = YiCommand(
            msgId = YiMsgIds.MSG_SET_SETTING,
            token = token,
            type = "wifi_ssid",
            param = ssid
        )
        val ssidResp = socketClient.sendCommand(ssidCmd)
        if (ssidResp == null || ssidResp.rval != 0) {
            return Result.failure(Exception("Wi-Fi 이름(SSID) 변경 실패 (코드: ${ssidResp?.rval})"))
        }

        // 2. Wi-Fi 비밀번호 변경
        val pwdCmd = YiCommand(
            msgId = YiMsgIds.MSG_SET_SETTING,
            token = token,
            type = "wifi_password",
            param = password
        )
        val pwdResp = socketClient.sendCommand(pwdCmd)
        if (pwdResp == null || pwdResp.rval != 0) {
            return Result.failure(Exception("Wi-Fi 비밀번호 변경 실패 (코드: ${pwdResp?.rval})"))
        }

        // 3. 로컬 상태 업데이트
        _cameraState.update {
            it.copy(
                settings = it.settings.copy(
                    wifiSsid = ssid,
                    wifiPassword = password
                )
            )
        }
        return Result.success(Unit)
    }

    override suspend fun formatSdCard(): Result<Unit> {
        val token = socketClient.getSessionToken()
        _cameraState.update { it.copy(isFormattingSdCard = true, formatResult = null) }
        val cmd = YiCommand(
            msgId = YiMsgIds.MSG_FORMAT_SD,
            token = token,
            param = "D:"
        )
        val resp = socketClient.sendCommand(cmd, timeoutMs = 20000)
        _cameraState.update { it.copy(isFormattingSdCard = false) }

        return if (resp != null && resp.rval == 0) {
            _cameraState.update { it.copy(formatResult = "SD 카드가 성공적으로 포맷되었습니다.") }
            refreshBatteryAndStorage()
            Result.success(Unit)
        } else {
            val err = "SD 카드 포맷 실패 (코드: ${resp?.rval})"
            _cameraState.update { it.copy(formatResult = err) }
            Result.failure(Exception(err))
        }
    }

    override fun clearFormatResult() {
        _cameraState.update { it.copy(formatResult = null) }
    }

    override suspend fun refreshBatteryAndStorage() {
        val token = socketClient.getSessionToken()
        if (token == 0) return

        // 1. 배터리 상태 질의
        socketClient.sendCommand(YiCommand(msgId = YiMsgIds.MSG_GET_BATTERY_STATUS, token = token))

        // 2. 가용 용량 질의 (type = "free")
        val freeResp = socketClient.sendCommand(
            YiCommand(msgId = YiMsgIds.MSG_QUERY_STORAGE, token = token, type = "free")
        )

        // 3. 전체 용량 질의 (type = "total")
        val totalResp = socketClient.sendCommand(
            YiCommand(msgId = YiMsgIds.MSG_QUERY_STORAGE, token = token, type = "total")
        )

        handleStorageResponse(freeResp, totalResp)
    }

    private fun handleStorageResponse(freeResp: com.github.kimymym.yicamera.data.model.YiResponse?, totalResp: com.github.kimymym.yicamera.data.model.YiResponse?) {
        if (freeResp == null && totalResp == null) return

        val freeRval = freeResp?.rval ?: 0
        val totalRval = totalResp?.rval ?: 0
        val errorRval = if (freeRval < 0) freeRval else if (totalRval < 0) totalRval else 0

        if (errorRval < 0) {
            val errorMsg = when (errorRval) {
                -4 -> "SD 카드가 삽입되지 않았습니다. (-4)"
                -14 -> "SD 카드 포맷이 필요합니다. (-14)"
                -17 -> "SD 카드 용량이 가득 찼습니다. (-17)"
                -24 -> "SD 카드 I/O 읽기 오류 (-24)"
                -9 -> "SD 조회 파라미터 오류 (-9)"
                else -> "SD 카드 오류 (코드: $errorRval)"
            }
            android.util.Log.e("YiCameraRepository", "SD Card Error: $errorMsg")
            _cameraState.update {
                it.copy(
                    storage = it.storage.copy(
                        status = "오류",
                        errorMessage = errorMsg
                    )
                )
            }
            return
        }

        // 응답 값 파싱 (카메라는 KB 단위로 반환함)
        val freeRaw = freeResp?.param?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val totalRaw = totalResp?.param?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

        // 100,000 이상이면 KB 단위로 간주하여 MB로 변환
        val freeMb = if (freeRaw > 100_000) freeRaw / 1024 else freeRaw
        val totalMb = if (totalRaw > 100_000) totalRaw / 1024 else totalRaw

        android.util.Log.d("YiCameraRepository", "SD Space: free=${freeMb}MB, total=${totalMb}MB")
        _cameraState.update {
            it.copy(
                storage = StorageInfo(
                    totalMb = totalMb,
                    freeMb = freeMb,
                    status = "정상",
                    errorMessage = null
                )
            )
        }
    }

    private fun startStatusPolling() {
        statusPollingJob?.cancel()
        statusPollingJob = repoScope.launch {
            while (isActive) {
                delay(10_000)
                refreshBatteryAndStorage()
            }
        }
    }

    private fun startRecordTimer() {
        recordTimerJob?.cancel()
        recordTimerJob = repoScope.launch {
            while (isActive) {
                delay(1000)
                _cameraState.update {
                    if (it.isRecording) {
                        it.copy(recordingDurationSec = it.recordingDurationSec + 1)
                    } else it
                }
            }
        }
    }

    override suspend fun fetchMediaList(): Result<List<CameraMediaItem>> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<CameraMediaItem>()

        // 1. 소켓 명령 msg_id: 1282 시도
        try {
            val token = socketClient.getSessionToken()
            if (token != 0) {
                // YI 카메라는 /tmp/fuse_d/DCIM/100MEDIA 디렉토리에 사진 및 동영상을 저장함
                val cmd = YiCommand(
                    msgId = YiMsgIds.MSG_LIST_FILES,
                    token = token,
                    param = "/tmp/fuse_d/DCIM/100MEDIA -D -S"
                )
                val resp = socketClient.sendCommand(cmd, timeoutMs = 5000)
                if (resp != null && resp.rval == 0 && resp.listing != null) {
                    val listingArray = resp.listing.jsonArray
                    for (itemElem in listingArray) {
                        val obj = itemElem.jsonObject
                        for ((filename, meta) in obj) {
                            if (filename.endsWith("/") || filename.startsWith(".")) continue
                            val metaStr = meta.jsonPrimitive.content
                            val parts = metaStr.split("|")
                            val sizeStr = parts.getOrNull(0)?.replace("bytes", "")?.trim()
                            val size = sizeStr?.toLongOrNull() ?: 0L
                            val date = parts.getOrNull(1)?.trim() ?: ""

                            val isVideo = filename.endsWith(".mp4", ignoreCase = true)
                            val isPhoto = filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true)
                            if (isVideo || isPhoto) {
                                mediaList.add(
                                    CameraMediaItem(
                                        name = filename,
                                        fullUrl = "http://192.168.42.1/DCIM/100MEDIA/$filename",
                                        thumbnailUrl = "http://192.168.42.1/DCIM/100MEDIA/$filename?type=thumb",
                                        screenUrl = "http://192.168.42.1/DCIM/100MEDIA/$filename?type=screen",
                                        sizeBytes = size,
                                        dateTime = date,
                                        mediaType = if (isVideo) MediaType.VIDEO else MediaType.PHOTO
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("YiCameraRepository", "Socket list files failed, trying HTTP fallback", e)
        }

        // 2. 소켓 조회 실패 또는 비어있을 시 HTTP 디렉토리 인덱스 Fallback
        if (mediaList.isEmpty()) {
            try {
                val url = java.net.URL("http://192.168.42.1/DCIM/100MEDIA/")
                val conn = networkManager.openConnection(url) as java.net.HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) {
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    val regex = Regex("""(?:href=["']|>)((?:YDXJ|[A-Za-z0-9_]+)\.(?:jpg|mp4|JPG|MP4))["'<]""")
                    val matches = regex.findAll(html)
                    val foundNames = mutableSetOf<String>()
                    for (m in matches) {
                        val filename = m.groupValues[1]
                        if (foundNames.add(filename)) {
                            val isVideo = filename.endsWith(".mp4", ignoreCase = true)
                            mediaList.add(
                                CameraMediaItem(
                                    name = filename,
                                    fullUrl = "http://192.168.42.1/DCIM/100MEDIA/$filename",
                                    thumbnailUrl = "http://192.168.42.1/DCIM/100MEDIA/$filename?type=thumb",
                                    screenUrl = "http://192.168.42.1/DCIM/100MEDIA/$filename?type=screen",
                                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.PHOTO
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("YiCameraRepository", "HTTP directory listing fallback failed", e)
            }
        }

        // 최신 촬영 파일(이름 역순 또는 날짜 역순) 우선 정렬
        val sorted = mediaList.sortedByDescending { it.name }
        Log.d("YiCameraRepository", "Fetched media files count: ${sorted.size}")
        Result.success(sorted)
    }

    override suspend fun downloadMediaFile(
        item: CameraMediaItem,
        onProgress: (Float) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL(item.fullUrl)
            val conn = networkManager.openConnection(url) as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 20000
            conn.requestMethod = "GET"

            val totalBytes = conn.contentLengthLong.takeIf { it > 0 } ?: item.sizeBytes
            val resolver = context.contentResolver
            val isVideo = item.isVideo

            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, item.name)
                put(
                    android.provider.MediaStore.MediaColumns.MIME_TYPE,
                    if (isVideo) "video/mp4" else "image/jpeg"
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) android.os.Environment.DIRECTORY_MOVIES + "/YiCamera"
                        else android.os.Environment.DIRECTORY_PICTURES + "/YiCamera"
                    )
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collectionUri = if (isVideo) {
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val fileUri = resolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry for ${item.name}"))

            var downloadedBytes = 0L
            val buffer = ByteArray(16384)

            conn.inputStream.use { input ->
                resolver.openOutputStream(fileUri)?.use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                    output.flush()
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(fileUri, contentValues, null, null)
            }

            onProgress(1f)
            Log.d("YiCameraRepository", "Successfully downloaded: ${item.name}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("YiCameraRepository", "Failed to download ${item.name}", e)
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean {
        return socketClient.isConnected() && _cameraState.value.connectionStatus == ConnectionStatus.AUTHENTICATED
    }

    override suspend fun checkAndRestoreState(): Boolean = withContext(Dispatchers.IO) {
        if (!socketClient.isConnected()) {
            Log.w("YiCameraRepository", "Socket disconnected, attempting reconnect...")
            val result = connect()
            return@withContext result.isSuccess
        }

        // 소켓이 살아있으면 최신 배터리 및 용량 조회
        try {
            refreshBatteryAndStorage()
            true
        } catch (e: Exception) {
            Log.e("YiCameraRepository", "Failed to refresh state", e)
            false
        }
    }
}
