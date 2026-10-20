package com.github.kimymym.yicamera.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kimymym.yicamera.R
import com.github.kimymym.yicamera.data.model.CameraDeviceState
import com.github.kimymym.yicamera.data.model.CameraMediaItem
import com.github.kimymym.yicamera.data.model.ConnectionStatus
import com.github.kimymym.yicamera.data.model.DownloadProgress
import com.github.kimymym.yicamera.data.model.ShootMode
import com.github.kimymym.yicamera.ui.main.components.ConnectionLostDialog
import com.github.kimymym.yicamera.ui.main.components.DownloadProgressDialog
import com.github.kimymym.yicamera.ui.main.components.FullscreenMediaViewer
import com.github.kimymym.yicamera.ui.main.components.TopInfoBar
import com.github.kimymym.yicamera.ui.main.tabs.GalleryTab
import com.github.kimymym.yicamera.ui.main.tabs.LivePreviewTab
import com.github.kimymym.yicamera.ui.main.tabs.SettingsTab

@Composable
fun MainRoute(
    onNavigateBackToConnect: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // 백그라운드 -> 포그라운드 복귀 시 카메라 연결 상태 확인 및 화면 유지
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onForegroundResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val isConnectionLost by viewModel.isConnectionLost.collectAsStateWithLifecycle()
    val isReconnecting by viewModel.isReconnecting.collectAsStateWithLifecycle()
    val isShutterProcessing by viewModel.isShutterProcessing.collectAsStateWithLifecycle()
    val showSdFormatDialog by viewModel.showSdFormatDialog.collectAsStateWithLifecycle()
    val sdFormatDialogReason by viewModel.sdFormatDialogReason.collectAsStateWithLifecycle()
    val galleryMedia by viewModel.galleryMedia.collectAsStateWithLifecycle()
    val isGalleryLoading by viewModel.isGalleryLoading.collectAsStateWithLifecycle()
    val selectedNames by viewModel.selectedMediaNames.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val fullscreenItem by viewModel.fullscreenPreviewItem.collectAsStateWithLifecycle()

    MainScreen(
        cameraState = cameraState,
        isConnectionLost = isConnectionLost,
        isReconnecting = isReconnecting,
        isShutterProcessing = isShutterProcessing,
        showSdFormatDialog = showSdFormatDialog,
        sdFormatDialogReason = sdFormatDialogReason,
        galleryMedia = galleryMedia,
        isGalleryLoading = isGalleryLoading,
        selectedNames = selectedNames,
        downloadProgress = downloadProgress,
        fullscreenItem = fullscreenItem,
        onModeSwitch = viewModel::onSwitchMode,
        onShutterClick = viewModel::onShutterClick,
        onStopRecording = viewModel::stopRecording,
        onSettingChange = viewModel::onSettingChange,
        onFormatSdCard = viewModel::onFormatSdCard,
        onDismissSdFormatDialog = viewModel::onDismissSdFormatDialog,
        onClearFormatResult = viewModel::clearFormatResult,
        onDisconnect = { viewModel.onDisconnect(onNavigateBackToConnect) },
        onRetryConnection = viewModel::retryConnection,
        onNavigateBackToConnect = onNavigateBackToConnect,
        onRefreshGallery = viewModel::loadGalleryMedia,
        onToggleSelect = viewModel::toggleItemSelection,
        onSelectAll = viewModel::selectAllMedia,
        onClearSelect = viewModel::clearMediaSelection,
        onOpenFullscreen = viewModel::openFullscreenPreview,
        onCloseFullscreen = viewModel::closeFullscreenPreview,
        onDownloadSelected = viewModel::downloadSelectedMedia,
        onDismissDownloadProgress = viewModel::dismissDownloadProgress,
        onToggleFlip = viewModel::onToggleVideoRotate,
        onChangeWifiConfig = viewModel::changeWifiConfig
    )
}

@Composable
fun MainScreen(
    cameraState: CameraDeviceState,
    isConnectionLost: Boolean,
    isReconnecting: Boolean,
    isShutterProcessing: Boolean,
    showSdFormatDialog: Boolean,
    sdFormatDialogReason: String?,
    galleryMedia: List<CameraMediaItem>,
    isGalleryLoading: Boolean,
    selectedNames: Set<String>,
    downloadProgress: DownloadProgress?,
    fullscreenItem: CameraMediaItem?,
    onModeSwitch: (ShootMode) -> Unit,
    onShutterClick: () -> Unit,
    onStopRecording: () -> Unit,
    onSettingChange: (type: String, value: String) -> Unit,
    onFormatSdCard: () -> Unit,
    onDismissSdFormatDialog: () -> Unit,
    onClearFormatResult: () -> Unit,
    onDisconnect: () -> Unit,
    onRetryConnection: () -> Unit,
    onNavigateBackToConnect: () -> Unit,
    onRefreshGallery: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelect: () -> Unit,
    onOpenFullscreen: (CameraMediaItem) -> Unit,
    onCloseFullscreen: () -> Unit,
    onDownloadSelected: () -> Unit,
    onDismissDownloadProgress: () -> Unit,
    onToggleFlip: () -> Unit,
    onChangeWifiConfig: (ssid: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showStopRecordDialog by remember { mutableStateOf(false) }

    // 1. 영상 녹화 중 Back 키 누르면 녹화 중지 팝업 노출
    BackHandler(enabled = cameraState.isRecording) {
        showStopRecordDialog = true
    }

    // 2. 대기 중 Back 키를 누르면 카메라 연결 해제 후 첫 Wi-Fi 연결 화면으로 복귀
    BackHandler(enabled = !cameraState.isRecording && !isConnectionLost) {
        onDisconnect()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopInfoBar(
                battery = cameraState.battery,
                storage = cameraState.storage,
                isConnected = (cameraState.connectionStatus == ConnectionStatus.AUTHENTICATED && !isConnectionLost)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.White
            ) {
                // 탭 0: 라이브 프리뷰
                NavigationBarItem(
                    selected = (selectedTab == 0),
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.tab_live_preview)) },
                    label = { Text(stringResource(R.string.tab_live_preview)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00E5FF),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888)
                    )
                )

                // 탭 1: 갤러리 (사진/영상 다운로드)
                NavigationBarItem(
                    selected = (selectedTab == 1),
                    onClick = {
                        selectedTab = 1
                        if (galleryMedia.isEmpty() && !isGalleryLoading) {
                            onRefreshGallery()
                        }
                    },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.tab_gallery)) },
                    label = { Text(stringResource(R.string.tab_gallery)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00E5FF),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888)
                    )
                )

                // 탭 2: 설정
                NavigationBarItem(
                    selected = (selectedTab == 2),
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.tab_settings)) },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00E5FF),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            when (selectedTab) {
                0 -> {
                    LivePreviewTab(
                        isViewfinderActive = (cameraState.isViewfinderActive && !isConnectionLost),
                        shootMode = cameraState.shootMode,
                        isRecording = cameraState.isRecording,
                        recordingDurationSec = cameraState.recordingDurationSec,
                        isShutterProcessing = isShutterProcessing,
                        isVideoRotateOn = cameraState.settings.isVideoRotateOn,
                        onToggleFlip = onToggleFlip,
                        onModeSwitch = onModeSwitch,
                        onShutterClick = onShutterClick
                    )
                }
                1 -> {
                    GalleryTab(
                        mediaList = galleryMedia,
                        isLoading = isGalleryLoading,
                        selectedNames = selectedNames,
                        onRefresh = onRefreshGallery,
                        onToggleSelect = onToggleSelect,
                        onSelectAll = onSelectAll,
                        onClearSelect = onClearSelect,
                        onItemClick = onOpenFullscreen,
                        onDownloadSelected = onDownloadSelected
                    )
                }
                2 -> {
                    SettingsTab(
                        settings = cameraState.settings,
                        storage = cameraState.storage,
                        isFormattingSdCard = cameraState.isFormattingSdCard,
                        formatResult = cameraState.formatResult,
                        onSettingChange = onSettingChange,
                        onChangeWifiConfig = onChangeWifiConfig,
                        onFormatSdCard = onFormatSdCard,
                        onDisconnect = onDisconnect
                    )
                }
            }
        }
    }

    // 영상 녹화 중 Back 키를 눌렀을 때 표시되는 확인 다이얼로그
    if (showStopRecordDialog) {
        AlertDialog(
            onDismissRequest = { showStopRecordDialog = false },
            title = { Text(stringResource(R.string.dialog_recording_title), color = Color.White) },
            text = { Text(stringResource(R.string.dialog_recording_message), color = Color(0xFFDDDDDD)) },
            confirmButton = {
                Button(
                    onClick = {
                        showStopRecordDialog = false
                        onStopRecording()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00))
                ) {
                    Text(stringResource(R.string.btn_stop_recording), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopRecordDialog = false }) {
                    Text(stringResource(R.string.btn_continue_recording), color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF242424)
        )
    }

    // 전체화면 미디어 뷰어 다이얼로그 (클릭 시 전체보기)
    fullscreenItem?.let { item ->
        FullscreenMediaViewer(
            item = item,
            onDismiss = onCloseFullscreen,
            onDownloadSingle = {
                onToggleSelect(it.name)
                onDownloadSelected()
            }
        )
    }

    // 다운로드 진행 다이얼로그
    downloadProgress?.let { progress ->
        DownloadProgressDialog(
            progress = progress,
            onDismiss = onDismissDownloadProgress
        )
    }

    // 카메라 연결 끊김 발생 시 다이얼로그 (재연결 또는 대기화면 전환)
    if (isConnectionLost) {
        ConnectionLostDialog(
            isReconnecting = isReconnecting,
            onRetry = onRetryConnection,
            onNavigateToConnect = onNavigateBackToConnect
        )
    }

    // 촬영 시 SD 카드 포맷 안내 다이얼로그 (촬영 시 -28, -14 에러 등 감지 시)
    if (showSdFormatDialog) {
        AlertDialog(
            onDismissRequest = onDismissSdFormatDialog,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dialog_sd_format_guide_title), color = Color.White)
                }
            },
            text = {
                Text(
                    text = if (sdFormatDialogReason != null) {
                        stringResource(R.string.dialog_sd_format_guide_dynamic_msg, sdFormatDialogReason)
                    } else {
                        stringResource(R.string.dialog_sd_format_guide_default_msg)
                    },
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTab = 2 // 설정 탭으로 바로 이동
                        onDismissSdFormatDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text(stringResource(R.string.btn_go_to_settings), color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissSdFormatDialog) {
                    Text(stringResource(R.string.btn_close), color = Color.White)
                }
            },
            containerColor = Color(0xFF242424)
        )
    }

    // SD 카드 포맷 진행 중 다이얼로그
    if (cameraState.isFormattingSdCard) {
        AlertDialog(
            onDismissRequest = { /* 포맷 중 임의 닫기 방지 */ },
            title = { Text(stringResource(R.string.dialog_sd_formatting_title), color = Color.White) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.dialog_sd_formatting_message),
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF242424)
        )
    }

    // SD 카드 포맷 완료 / 실패 안내 다이얼로그
    cameraState.formatResult?.let { result ->
        val isSuccess = result.contains("성공") || result.contains("successfully", ignoreCase = true)
        AlertDialog(
            onDismissRequest = onClearFormatResult,
            title = {
                Text(
                    text = if (isSuccess) stringResource(R.string.dialog_sd_format_success_title) else stringResource(R.string.dialog_sd_format_failed_title),
                    color = if (isSuccess) Color(0xFF00E676) else Color(0xFFFF5252)
                )
            },
            text = {
                Text(
                    text = if (isSuccess) {
                        stringResource(R.string.settings_sd_format_success) + stringResource(R.string.dialog_sd_format_success_sub)
                    } else {
                        stringResource(R.string.settings_sd_format_failed, result)
                    },
                    color = Color(0xFFDDDDDD),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onClearFormatResult,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) Color(0xFF00E5FF) else Color(0xFF333333)
                    )
                ) {
                    Text(stringResource(R.string.btn_confirm), color = if (isSuccess) Color.Black else Color.White)
                }
            },
            containerColor = Color(0xFF242424)
        )
    }
}
