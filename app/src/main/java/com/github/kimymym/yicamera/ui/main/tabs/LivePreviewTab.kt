package com.github.kimymym.yicamera.ui.main.tabs

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.kimymym.yicamera.data.model.ShootMode
import com.github.kimymym.yicamera.ui.main.components.ExoPlayerView
import com.github.kimymym.yicamera.ui.main.components.GridOverlay

@Composable
fun LivePreviewTab(
    isViewfinderActive: Boolean,
    shootMode: ShootMode,
    isRecording: Boolean,
    recordingDurationSec: Long,
    isShutterProcessing: Boolean = false,
    isVideoRotateOn: Boolean = false,
    onToggleFlip: () -> Unit = {},
    onModeSwitch: (ShootMode) -> Unit,
    onShutterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showGrid by remember { mutableStateOf(true) }

    // 영상 녹화 중에는 화면이 항상 켜져있도록 Screen On 플래그 적용
    DisposableEffect(isRecording) {
        val activity = context as? Activity
        if (isRecording) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. 실시간 RTSP 뷰파인더 화면
        ExoPlayerView(
            isActive = isViewfinderActive,
            modifier = Modifier.fillMaxSize()
        )

        // 2. 3*4 보조 그리드 선 오버레이
        GridOverlay(
            isVisible = showGrid,
            columns = 3,
            rows = 4,
            modifier = Modifier.fillMaxSize()
        )

        // 3. 상단 우측 보조 도구 (영상 Flip 회전 토글, 그리드 토글)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Flip (상하 180도 반전) 토글 버튼
            IconButton(
                onClick = onToggleFlip,
                modifier = Modifier
                    .background(
                        if (isVideoRotateOn) Color(0xCC00838F) else Color(0x88000000),
                        shape = CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = "영상 Flip 회전",
                    tint = if (isVideoRotateOn) Color(0xFF00E5FF) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 보조선 그리드 토글 버튼
            IconButton(
                onClick = { showGrid = !showGrid },
                modifier = Modifier
                    .background(Color(0x88000000), shape = CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Toggle Grid",
                    tint = if (showGrid) Color(0xFF00E5FF) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 4. 녹화 중 타이머 인디케이터 (화면 상단 중앙)
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            val minutes = recordingDurationSec / 60
            val seconds = recordingDurationSec % 60
            val formattedTime = String.format("%02d:%02d", minutes, seconds)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xCCFF0000), shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REC $formattedTime",
                    color = Color.White,
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // 5. 하단 카메라 제어 컨트롤 패널 (모드 전환 + 대형 셔터 버튼)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xAA000000))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 사진 / 동영상 모드 선택 스위치
            Row(
                modifier = Modifier
                    .background(Color(0x66333333), shape = RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // 사진 모드
                ModeButton(
                    title = "사진",
                    icon = Icons.Default.CameraAlt,
                    isSelected = (shootMode == ShootMode.PHOTO),
                    onClick = { onModeSwitch(ShootMode.PHOTO) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 동영상 모드
                ModeButton(
                    title = "동영상",
                    icon = Icons.Default.Videocam,
                    isSelected = (shootMode == ShootMode.VIDEO),
                    onClick = { onModeSwitch(ShootMode.VIDEO) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 셔터 버튼
            ShutterButton(
                shootMode = shootMode,
                isRecording = isRecording,
                isProcessing = isShutterProcessing,
                onClick = onShutterClick
            )
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ShutterButton(
    shootMode: ShootMode,
    isRecording: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (isProcessing) 0.5f else 1.0f

    Box(
        modifier = Modifier
            .size(76.dp)
            .border(width = 4.dp, color = Color.White.copy(alpha = alpha), shape = CircleShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (shootMode == ShootMode.PHOTO) {
            // 사진 셔터: 흰색 원
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
                    .clickable(
                        enabled = !isProcessing,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
            )
        } else {
            // 동영상 셔터: 녹화 중이면 중앙의 작은 빨간 네모(Stop), 아니면 빨간 원
            Box(
                modifier = Modifier
                    .size(if (isRecording) 28.dp else 58.dp)
                    .clip(if (isRecording) RoundedCornerShape(6.dp) else CircleShape)
                    .background(Color(0xFFFF3D00).copy(alpha = alpha))
                    .clickable(
                        enabled = !isProcessing,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
            )
        }
    }
}
