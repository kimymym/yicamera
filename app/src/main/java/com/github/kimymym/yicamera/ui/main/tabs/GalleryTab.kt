package com.github.kimymym.yicamera.ui.main.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.github.kimymym.yicamera.R
import com.github.kimymym.yicamera.data.model.CameraMediaItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryTab(
    mediaList: List<CameraMediaItem>,
    isLoading: Boolean,
    selectedNames: Set<String>,
    onRefresh: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelect: () -> Unit,
    onItemClick: (CameraMediaItem) -> Unit,
    onDownloadSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isManualSelectMode by remember { mutableStateOf(false) }
    val isSelectionMode = isManualSelectMode || selectedNames.isNotEmpty()

    // 갤러리 탭 진입 시 비어있으면 자동 조회
    LaunchedEffect(Unit) {
        if (mediaList.isEmpty() && !isLoading) {
            onRefresh()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 갤러리 헤더 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSelectionMode) {
                    // 다중 선택 모드 헤더
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.gallery_selected_count, selectedNames.size),
                            color = Color(0xFF00E5FF),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        TextButton(onClick = onSelectAll) {
                            Text(stringResource(R.string.btn_select_all), color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = {
                                isManualSelectMode = false
                                onClearSelect()
                            }
                        ) {
                            Text(stringResource(R.string.btn_cancel), color = Color(0xFFAAAAAA), fontSize = 13.sp)
                        }
                    }
                } else {
                    // 일반 모드 헤더
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.gallery_title),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${mediaList.size})",
                            color = Color(0xFF888888),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 새로고침 버튼
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.btn_refresh),
                                tint = Color(0xFF00E5FF)
                            )
                        }

                        // 선택 모드 전환 버튼
                        IconButton(
                            onClick = {
                                isManualSelectMode = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = stringResource(R.string.btn_select_mode),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // 본체: 미디어 리스트 또는 빈 상태 / 로딩 상태
            if (isLoading && mediaList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.gallery_loading),
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (mediaList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.gallery_empty),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                        ) {
                            Text(stringResource(R.string.btn_refresh), color = Color(0xFF00E5FF))
                        }
                    }
                }
            } else {
                // 3열 상하 스크롤 그리드
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(mediaList, key = { it.name }) { item ->
                        val isSelected = selectedNames.contains(item.name)

                        MediaGridItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelect(item.name)
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onLongClick = {
                                isManualSelectMode = true
                                onToggleSelect(item.name)
                            }
                        )
                    }
                }
            }
        }

        // 하단 중앙 다운로드 플로팅 액션 버튼 (하나 이상 선택 시 노출)
        AnimatedVisibility(
            visible = selectedNames.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Button(
                onClick = onDownloadSelected,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = stringResource(R.string.desc_download),
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.gallery_btn_download_count, selectedNames.size),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItem(
    item: CameraMediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF222222))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(2.dp))
                } else Modifier
            )
    ) {
        // 썸네일 이미지 로딩 (비디오는 내장 .THM 우선, 이미지는 ?type=thumb)
        val initialThumbUrl = if (item.isVideo) item.thmUrl else item.thumbnailUrl
        SubcomposeAsyncImage(
            model = initialThumbUrl,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF555555)
                    )
                }
            },
            error = {
                // 비디오 .THM 실패 시 소문자 .thm 또는 type=thumb 시도
                if (item.isVideo) {
                    SubcomposeAsyncImage(
                        model = item.thmUrlLower,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            SubcomposeAsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF2A2A2A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFF777777),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            )
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        )

        // 동영상인 경우: 영상마크 + duration 단일 오버레이 (간소화)
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color(0x99000000), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.desc_video),
                        tint = Color(0xFFFF3D00),
                        modifier = Modifier.size(13.dp)
                    )
                    if (item.formattedDuration.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.formattedDuration,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        // 이미지는 용량 표시 숨김 (요청사항 반영)

        // 선택 모드 또는 선택된 상태 오버레이
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) Color(0x5500E5FF) else Color(0x33000000)
                    )
            )

            // 우측 상단 체크박스 아이콘
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.desc_selected),
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.Black, shape = CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = stringResource(R.string.desc_not_selected),
                        tint = Color(0xCCFFFFFF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
