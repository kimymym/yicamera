package com.github.kimymym.yicamera.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConnectionLostDialog(
    isReconnecting: Boolean,
    onRetry: () -> Unit,
    onNavigateToConnect: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* 강제 모달 */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF242424), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "연결 끊김",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "카메라 연결 끊김",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "카메라와의 Wi-Fi 또는 소켓 연결이 끊어졌습니다.\n카메라 전원 및 Wi-Fi 연결을 확인해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onNavigateToConnect,
                        modifier = Modifier.weight(1f),
                        enabled = !isReconnecting,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("대기화면으로", color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        enabled = !isReconnecting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isReconnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                        } else {
                            Text("재연결", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
