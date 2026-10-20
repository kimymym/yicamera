package com.github.kimymym.yicamera.ui.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.kimymym.yicamera.data.model.CameraSettings
import com.github.kimymym.yicamera.data.model.StorageInfo

@Composable
fun SettingsTab(
    settings: CameraSettings,
    storage: StorageInfo,
    isFormattingSdCard: Boolean,
    formatResult: String?,
    onSettingChange: (type: String, value: String) -> Unit,
    onChangeWifiConfig: (ssid: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onFormatSdCard: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFormatDialog by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }
    var wifiChangeSuccessMsg by remember { mutableStateOf<String?>(null) }

    if (showFormatDialog) {
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SD 카드 포맷", color = Color.White)
                }
            },
            text = {
                Text(
                    text = "SD 카드에 저장된 모든 사진과 동영상이 영구적으로 삭제됩니다.\n계속 진행하시겠습니까?",
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFormatDialog = false
                        onFormatSdCard()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("포맷 실행", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFormatDialog = false }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF242424)
        )
    }

    if (showWifiDialog) {
        WifiConfigDialog(
            currentSsid = settings.wifiSsid,
            currentPassword = settings.wifiPassword,
            onDismiss = { showWifiDialog = false },
            onApply = { newSsid, newPwd, onDone, onErr ->
                onChangeWifiConfig(newSsid, newPwd, {
                    onDone()
                    showWifiDialog = false
                    wifiChangeSuccessMsg = "Wi-Fi 설정이 성공적으로 변경되었습니다.\n카메라 Wi-Fi가 재부팅되면 스마트폰 Wi-Fi 설정에서 새 비밀번호로 다시 연결해주세요."
                }, onErr)
            }
        )
    }

    if (wifiChangeSuccessMsg != null) {
        AlertDialog(
            onDismissRequest = { wifiChangeSuccessMsg = null },
            title = { Text("Wi-Fi 변경 완료", color = Color(0xFF00E5FF)) },
            text = { Text(wifiChangeSuccessMsg ?: "", color = Color(0xFFDDDDDD), lineHeight = 20.sp) },
            confirmButton = {
                Button(
                    onClick = { wifiChangeSuccessMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("확인", color = Color.Black)
                }
            },
            containerColor = Color(0xFF242424)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 촬영 설정
        item {
            Text(
                text = "촬영 설정",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SettingDropdownItem(
                        title = "동영상 해상도",
                        currentValue = settings.videoResolution,
                        options = settings.videoResolutionOptions,
                        onSelect = { onSettingChange("video_resolution", it) }
                    )
                    HorizontalDivider(color = Color(0xFF2C2C2C))
                    SettingDropdownItem(
                        title = "사진 해상도",
                        currentValue = settings.photoResolution,
                        options = settings.photoResolutionOptions,
                        onSelect = { onSettingChange("photo_size", it) }
                    )
                    HorizontalDivider(color = Color(0xFF2C2C2C))
                    SettingDropdownItem(
                        title = "영상 상하 반전 (Flip)",
                        currentValue = if (settings.isVideoRotateOn) "켜짐 (180°)" else "꺼짐 (기본)",
                        options = listOf("꺼짐 (기본)", "켜짐 (180°)"),
                        onSelect = { selected ->
                            val value = if (selected.startsWith("켜짐")) "on" else "off"
                            onSettingChange("video_rotate", value)
                        }
                    )
                }
            }
        }

        // 2. 카메라 시스템 설정
        item {
            Text(
                text = "카메라 시스템 설정",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SettingDropdownItem(
                        title = "비프음 볼륨",
                        currentValue = settings.buzzerVolume,
                        options = settings.buzzerVolumeOptions,
                        onSelect = { onSettingChange("buzzer_volume", it) }
                    )
                    HorizontalDivider(color = Color(0xFF2C2C2C))
                    SettingDropdownItem(
                        title = "LED 인디케이터",
                        currentValue = settings.ledMode,
                        options = settings.ledModeOptions,
                        onSelect = { onSettingChange("led_mode", it) }
                    )
                    HorizontalDivider(color = Color(0xFF2C2C2C))
                    SettingDropdownItem(
                        title = "자동 전원 끄기",
                        currentValue = settings.autoPowerOff,
                        options = settings.autoPowerOffOptions,
                        onSelect = { onSettingChange("auto_power_off", it) }
                    )
                }
            }
        }

        // 3. Wi-Fi 및 네트워크 설정
        item {
            Text(
                text = "Wi-Fi 및 네트워크 설정",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = settings.wifiSsid.ifEmpty { "YDXJ_Camera" },
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "비밀번호: " + if (settings.wifiPassword.isNotEmpty()) "••••••••" else "미설정",
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showWifiDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("변경", color = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }

        // 4. 메모리 관리 (SD 카드 포맷)
        item {
            Text(
                text = "메모리 관리",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 오류 메시지 배너
                    if (storage.errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x33FF5252), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = storage.errorMessage,
                                color = Color(0xFFFF8A80),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 가용용량 / 전체용량 표시
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (storage.totalMb > 0) {
                                "가용: ${storage.freeGbFormatted} / 전체: ${storage.totalGbFormatted}"
                            } else if (storage.freeMb > 0) {
                                "가용 용량: ${storage.freeGbFormatted}"
                            } else {
                                "용량 정보 확인 중..."
                            },
                            color = Color(0xFF00E5FF),
                            fontSize = 14.sp
                        )

                        if (storage.totalMb > 0) {
                            Text(
                                text = "${((1f - storage.usageRatio) * 100).toInt()}% 여유",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (storage.totalMb > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { storage.usageRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0xFF333333)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF2C2C2C))
                    Spacer(modifier = Modifier.height(14.dp))

                    // SD 카드 포맷 영역
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("SD 카드 포맷", color = Color.White, fontSize = 15.sp)
                                Text("모든 데이터 초기화", color = Color(0xFF888888), fontSize = 12.sp)
                            }
                        }

                        if (isFormattingSdCard) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF5252),
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            OutlinedButton(
                                onClick = { showFormatDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("포맷", color = Color(0xFFFF5252))
                            }
                        }
                    }

                    if (formatResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = formatResult,
                            color = if (formatResult.contains("성공")) Color(0xFF00E676) else Color(0xFFFF5252),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 5. 카메라 연결 해제
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Disconnect",
                    tint = Color(0xFFFF5252)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("카메라 연결 해제", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SettingDropdownItem(
    title: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currentValue, color = Color(0xFF00E5FF), fontSize = 14.sp)
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF888888)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF262626))
            ) {
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("옵션 로딩중...", color = Color.Gray) },
                        onClick = { expanded = false }
                    )
                } else {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    color = if (option == currentValue) Color(0xFF00E5FF) else Color.White
                                )
                            },
                            onClick = {
                                expanded = false
                                onSelect(option)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiConfigDialog(
    currentSsid: String,
    currentPassword: String,
    onDismiss: () -> Unit,
    onApply: (newSsid: String, newPassword: String, onDone: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    var ssid by remember { mutableStateOf(currentSsid.ifEmpty { "YDXJ_Camera" }) }
    var password by remember { mutableStateOf(currentPassword.ifEmpty { "1234567890" }) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("카메라 Wi-Fi 설정 변경", color = Color.White)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "카메라의 Wi-Fi 이름(SSID)과 접속 비밀번호를 변경합니다.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = ssid,
                    onValueChange = {
                        ssid = it
                        errorMessage = null
                    },
                    label = { Text("Wi-Fi 이름 (SSID)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF555555),
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("비밀번호 (최소 8자)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF555555),
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 경고 문구
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33FFB300), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "주의: 변경 후 카메라 AP가 재시작되어 연결이 끊어집니다. 스마트폰 Wi-Fi 설정에서 새 비밀번호로 다시 연결해주세요.",
                        color = Color(0xFFFFE082),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedSsid = ssid.trim()
                    val trimmedPwd = password.trim()
                    if (trimmedSsid.isEmpty()) {
                        errorMessage = "Wi-Fi 이름을 입력해주세요."
                        return@Button
                    }
                    if (trimmedPwd.length < 8) {
                        errorMessage = "비밀번호는 최소 8자 이상이어야 합니다."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    onApply(trimmedSsid, trimmedPwd, { isLoading = false }, { err ->
                        isLoading = false
                        errorMessage = err
                    })
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("변경 중...", color = Color.Black)
                } else {
                    Text("변경 적용", color = Color.Black)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("취소", color = Color.White)
            }
        },
        containerColor = Color(0xFF242424)
    )
}
