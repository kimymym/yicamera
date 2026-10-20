package com.github.kimymym.yicamera.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kimymym.yicamera.data.model.ConnectionStatus
import com.github.kimymym.yicamera.data.repository.YiCameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val repository: YiCameraRepository
) : ViewModel() {

    private val _isConnecting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ConnectUiState> = combine(
        repository.cameraState,
        repository.isWifiConnected,
        _isConnecting,
        _errorMessage
    ) { cameraState, isWifi, isConnecting, errorMsg ->
        when {
            errorMsg != null -> ConnectUiState.Error(errorMsg)
            isConnecting || cameraState.connectionStatus == ConnectionStatus.CONNECTING_SOCKET -> ConnectUiState.Connecting
            cameraState.connectionStatus == ConnectionStatus.AUTHENTICATED -> ConnectUiState.Success
            else -> ConnectUiState.Idle(isWifiConnected = isWifi)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectUiState.Idle(isWifiConnected = false)
    )

    fun connectToCamera() {
        viewModelScope.launch {
            _isConnecting.value = true
            _errorMessage.value = null
            val result = repository.connect()
            _isConnecting.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "카메라 연결에 실패했습니다."
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
