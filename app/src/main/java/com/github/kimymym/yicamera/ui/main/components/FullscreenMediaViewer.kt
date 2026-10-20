package com.github.kimymym.yicamera.ui.main.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.github.kimymym.yicamera.data.model.CameraMediaItem

@OptIn(UnstableApi::class)
@Composable
fun FullscreenMediaViewer(
    item: CameraMediaItem,
    onDismiss: () -> Unit,
    onDownloadSingle: (CameraMediaItem) -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 본체: 미디어 뷰어 (사진 또는 영상)
            if (item.isVideo) {
                // ExoPlayer 비디오 재생기
                val exoPlayer = remember(item.fullUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(item.fullUrl))
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(exoPlayer) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                )
            } else {
                // 사진 고화질 뷰어 (스크린 프리뷰 우선 로딩으로 빠른 표시)
                val imageModel = if (item.screenUrl.isNotEmpty()) item.screenUrl else item.fullUrl
                SubcomposeAsyncImage(
                    model = imageModel,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00E5FF))
                        }
                    },
                    error = {
                        // 원본 로딩 실패 시 썸네일로 fallback 시도
                        SubcomposeAsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }

            // 상단 오버레이 바 (닫기 버튼, 파일명, 다운로드 버튼)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0x99000000))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color(0x44FFFFFF), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.Photo,
                            contentDescription = null,
                            tint = if (item.isVideo) Color(0xFFFF3D00) else Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                    if (item.formattedSize.isNotEmpty() || item.dateTime.isNotEmpty()) {
                        Text(
                            text = listOf(item.formattedSize, item.dateTime).filter { it.isNotEmpty() }.joinToString(" • "),
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = { onDownloadSingle(item) },
                    modifier = Modifier
                        .background(Color(0x44FFFFFF), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "다운로드",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
