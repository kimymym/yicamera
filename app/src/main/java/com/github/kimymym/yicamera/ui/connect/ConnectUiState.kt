package com.github.kimymym.yicamera.ui.connect

import com.github.kimymym.yicamera.data.model.ConnectionStatus

sealed interface ConnectUiState {
    data class Idle(val isWifiConnected: Boolean) : ConnectUiState
    data object Connecting : ConnectUiState
    data object Success : ConnectUiState
    data class Error(val message: String) : ConnectUiState
}
