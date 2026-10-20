package com.github.kimymym.yicamera.ui.main.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    rtspUrl: String = "rtsp://192.168.42.1/live",
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 초저지연 버퍼링 제어 (DefaultLoadControl)
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 500,
                /* maxBufferMs = */ 1500,
                /* bufferForPlaybackMs = */ 150,
                /* bufferForPlaybackAfterRebufferMs = */ 250
            )
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // RTSP MediaSource 구성 (RTP over TCP 강제)
    LaunchedEffect(rtspUrl, isActive) {
        if (isActive) {
            errorMessage = null
            isBuffering = true

            val mediaItem = MediaItem.fromUri(rtspUrl)
            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(false) // YI 카메라는 UDP(RTP/AVP)만 지원함
                .setDebugLoggingEnabled(true)
                .createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            exoPlayer.stop()
        }
    }

    // 플레이어 이벤트 리스너
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                android.util.Log.d("YiExoPlayer", "Playback state changed: $playbackState")
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                android.util.Log.e("YiExoPlayer", "Player error occurred: ${error.errorCodeName}", error)
                errorMessage = "스트림 연결 실패 (${error.errorCodeName}: ${error.message})"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 라이프사이클 연동 (백그라운드 진입 시 정지, 복귀 시 RTSP 재연결)
    DisposableEffect(lifecycleOwner, isActive, rtspUrl) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.stop()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isActive) {
                        errorMessage = null
                        isBuffering = true
                        val mediaItem = MediaItem.fromUri(rtspUrl)
                        val mediaSource = RtspMediaSource.Factory()
                            .setForceUseRtpTcp(false)
                            .setDebugLoggingEnabled(true)
                            .createMediaSource(mediaItem)
                        exoPlayer.setMediaSource(mediaSource)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(color = Color(0xFF00E5FF))
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.White
            )
        }
    }
}
