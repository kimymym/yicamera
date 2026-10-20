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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.kimymym.yicamera.data.model.DownloadProgress

@Composable
fun DownloadProgressDialog(
    progress: DownloadProgress,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = {
        if (progress.isCompleted || progress.error != null) {
            onDismiss()
        }
    }) {
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
                if (progress.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "완료",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "다운로드 완료",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "총 ${progress.total}개의 파일이 기기 갤러리에 저장되었습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFAAAAAA)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("확인", color = Color.Black)
                    }
                } else if (progress.error != null) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "오류",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "다운로드 중 오류",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progress.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFAAAAAA)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("닫기", color = Color.White)
                    }
                } else {
                    Text(
                        text = "미디어 다운로드 중 (${progress.current}/${progress.total})",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progress.currentFileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress.currentFileProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF444444),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(progress.currentFileProgress * 100).toInt()}%",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
