package com.github.kimymym.yicamera.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.kimymym.yicamera.data.model.BatteryInfo
import com.github.kimymym.yicamera.data.model.StorageInfo

@Composable
fun TopInfoBar(
    battery: BatteryInfo,
    storage: StorageInfo,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC1E1E1E))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
        // 왼쪽: 카메라 브랜드 및 연결 상태
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF5252),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "YI Action Cam",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }

        // 오른쪽: 배터리 & SD카드 용량
        Row(verticalAlignment = Alignment.CenterVertically) {
            // SD카드 용량
            Icon(
                imageVector = Icons.Default.SdCard,
                contentDescription = "SD Card",
                tint = Color(0xFFB0BEC5),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    storage.errorMessage != null -> "SD 오류"
                    storage.freeMb > 0 -> storage.freeGbFormatted
                    else -> "SD 확인중"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (storage.errorMessage != null) Color(0xFFFF5252) else Color.White,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // 배터리
            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = "Battery",
                tint = if (battery.percentage <= 20) Color(0xFFFF5252) else Color(0xFF00E676),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${battery.percentage}%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
}
