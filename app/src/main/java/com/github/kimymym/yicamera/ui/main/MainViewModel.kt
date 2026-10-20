package com.github.kimymym.yicamera.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kimymym.yicamera.data.model.CameraDeviceState
import com.github.kimymym.yicamera.data.model.CameraMediaItem
import com.github.kimymym.yicamera.data.model.CameraShootException
import com.github.kimymym.yicamera.data.model.DownloadProgress
import com.github.kimymym.yicamera.data.model.ShootMode
import com.github.kimymym.yicamera.data.repository.YiCameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: YiCameraRepository
) : ViewModel() {

    val cameraState: StateFlow<CameraDeviceState> = repository.cameraState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CameraDeviceState()
        )

    // SD 카드 포맷 필요 다이얼로그 상태 (촬영 시 -28 에러 등 발생 시)
    private val _showSdFormatDialog = MutableStateFlow(false)
    val showSdFormatDialog: StateFlow<Boolean> = _showSdFormatDialog.asStateFlow()

    private val _sdFormatDialogReason = MutableStateFlow<String?>(null)
    val sdFormatDialogReason: StateFlow<String?> = _sdFormatDialogReason.asStateFlow()

    // 카메라 연결 끊김 및 재연결 상태
    private val _isConnectionLost = MutableStateFlow(false)
    val isConnectionLost: StateFlow<Boolean> = _isConnectionLost.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    // 갤러리 미디어 목록 및 상태
    private val _galleryMedia = MutableStateFlow<List<CameraMediaItem>>(emptyList())
    val galleryMedia: StateFlow<List<CameraMediaItem>> = _galleryMedia.asStateFlow()

    private val _isGalleryLoading = MutableStateFlow(false)
    val isGalleryLoading: StateFlow<Boolean> = _isGalleryLoading.asStateFlow()

    // 멀티 선택된 파일 이름 Set
    private val _selectedMediaNames = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaNames: StateFlow<Set<String>> = _selectedMediaNames.asStateFlow()

    // 다운로드 진행 상태
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    // 클릭 시 전체보기 대상 미디어
    private val _fullscreenPreviewItem = MutableStateFlow<CameraMediaItem?>(null)
    val fullscreenPreviewItem: StateFlow<CameraMediaItem?> = _fullscreenPreviewItem.asStateFlow()

    /**
     * 앱이 BG에서 FG로 복귀할 때 상태 확인 및 복원
     */
    fun onForegroundResume() {
        viewModelScope.launch {
            val isAlive = repository.checkAndRestoreState()
            if (!isAlive) {
                _isConnectionLost.value = true
            } else {
                _isConnectionLost.value = false
            }
        }
    }

    /**
     * 연결 끊김 다이얼로그에서 [재연결] 클릭 시 실행
     */
    fun retryConnection() {
        viewModelScope.launch {
            _isReconnecting.value = true
            val result = repository.connect()
            if (result.isSuccess) {
                _isConnectionLost.value = false
            }
            _isReconnecting.value = false
        }
    }

    // 셔터 명령 처리 중 상태 (연타 방지)
    private val _isShutterProcessing = MutableStateFlow(false)
    val isShutterProcessing: StateFlow<Boolean> = _isShutterProcessing.asStateFlow()

    fun onSwitchMode(mode: ShootMode) {
        if (_isShutterProcessing.value) return
        viewModelScope.launch {
            repository.switchMode(mode)
        }
    }

    fun onShutterClick() {
        if (_isShutterProcessing.value) return
        val state = cameraState.value
        _isShutterProcessing.value = true

        viewModelScope.launch {
            try {
                if (state.shootMode == ShootMode.PHOTO) {
                    val result = repository.takePhoto()
                    if (result.isFailure) {
                        handleShootFailure(result.exceptionOrNull())
                    }
                } else {
                    if (state.isRecording) {
                        val result = repository.stopRecording()
                        if (result.isFailure) {
                            handleShootFailure(result.exceptionOrNull())
                        }
                    } else {
                        val result = repository.startRecording()
                        if (result.isSuccess) {
                            // 녹화 시작 후 카메라 인코더 파이프라인 안정화를 위해 1.5초 쿨다운
                            kotlinx.coroutines.delay(1500)
                        } else {
                            handleShootFailure(result.exceptionOrNull())
                        }
                    }
                }
            } finally {
                _isShutterProcessing.value = false
            }
        }
    }

    private fun handleShootFailure(throwable: Throwable?) {
        val rval = (throwable as? CameraShootException)?.rval
        val msg = throwable?.message ?: ""
        val isFormatNeeded = (throwable as? CameraShootException)?.isSdFormatRequired == true
                || rval == -28 || rval == -14 || rval == -24
                || msg.contains("-28") || msg.contains("-14")

        if (isFormatNeeded) {
            _sdFormatDialogReason.value = if (rval != null) "code: $rval" else "-28"
            _showSdFormatDialog.value = true
        }
    }

    fun onDismissSdFormatDialog() {
        _showSdFormatDialog.value = false
        _sdFormatDialogReason.value = null
    }

    fun clearFormatResult() {
        repository.clearFormatResult()
    }

    fun stopRecording() {
        if (_isShutterProcessing.value) return
        _isShutterProcessing.value = true
        viewModelScope.launch {
            try {
                repository.stopRecording()
            } finally {
                _isShutterProcessing.value = false
            }
        }
    }

    fun onSettingChange(type: String, value: String) {
        viewModelScope.launch {
            repository.setSetting(type, value)
        }
    }

    fun onToggleVideoRotate() {
        val current = cameraState.value.settings.isVideoRotateOn
        val next = if (current) "off" else "on"
        onSettingChange("video_rotate", next)
    }

    fun changeWifiConfig(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.setWifiConfig(ssid, password)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Wi-Fi 설정 변경 실패")
            }
        }
    }

    fun onFormatSdCard() {
        viewModelScope.launch {
            repository.formatSdCard()
        }
    }

    fun onDisconnect(onDisconnected: () -> Unit) {
        viewModelScope.launch {
            repository.disconnect()
            onDisconnected()
        }
    }

    // --- 갤러리 관련 메서드 ---

    fun loadGalleryMedia() {
        viewModelScope.launch {
            _isGalleryLoading.value = true
            val result = repository.fetchMediaList()
            if (result.isSuccess) {
                _galleryMedia.value = result.getOrDefault(emptyList())
            }
            _isGalleryLoading.value = false
        }
    }

    fun toggleItemSelection(name: String) {
        val current = _selectedMediaNames.value.toMutableSet()
        if (current.contains(name)) {
            current.remove(name)
        } else {
            current.add(name)
        }
        _selectedMediaNames.value = current
    }

    fun selectAllMedia() {
        val allNames = _galleryMedia.value.map { it.name }.toSet()
        _selectedMediaNames.value = allNames
    }

    fun clearMediaSelection() {
        _selectedMediaNames.value = emptySet()
    }

    fun openFullscreenPreview(item: CameraMediaItem) {
        _fullscreenPreviewItem.value = item
    }

    fun closeFullscreenPreview() {
        _fullscreenPreviewItem.value = null
    }

    fun downloadSelectedMedia() {
        val selected = _selectedMediaNames.value
        val itemsToDownload = _galleryMedia.value.filter { it.name in selected }
        if (itemsToDownload.isEmpty()) return

        viewModelScope.launch {
            val total = itemsToDownload.size
            for ((index, item) in itemsToDownload.withIndex()) {
                _downloadProgress.value = DownloadProgress(
                    current = index + 1,
                    total = total,
                    currentFileName = item.name,
                    currentFileProgress = 0f
                )

                val result = repository.downloadMediaFile(item) { progress ->
                    _downloadProgress.value = _downloadProgress.value?.copy(
                        currentFileProgress = progress
                    )
                }

                if (result.isFailure) {
                    _downloadProgress.value = _downloadProgress.value?.copy(
                        error = "다운로드 실패: ${item.name}"
                    )
                }
            }

            // 완료 알림
            _downloadProgress.value = _downloadProgress.value?.copy(
                isCompleted = true
            )
            // 다운로드 완료 후 선택 해제
            clearMediaSelection()
        }
    }

    fun dismissDownloadProgress() {
        _downloadProgress.value = null
    }
}
