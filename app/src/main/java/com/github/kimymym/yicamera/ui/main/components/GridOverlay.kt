package com.github.kimymym.yicamera.ui.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 카메라 촬영 구도를 위한 보조선 오버레이 (3x4 그리드)
 * 가로 3칸(수직선 2개), 세로 4칸(수평선 3개) 또는 가로/세로 비율에 맞춘 그리드 라인
 */
@Composable
fun GridOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    rows: Int = 4,
    gridColor: Color = Color(0x66FFFFFF),
    strokeWidthDp: Float = 1f
) {
    if (!isVisible) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val strokePx = strokeWidthDp.dp.toPx()

        // 수직선 (Columns - 1개)
        val colStep = width / columns
        for (i in 1 until columns) {
            val x = colStep * i
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = strokePx
            )
        }

        // 수평선 (Rows - 1개)
        val rowStep = height / rows
        for (j in 1 until rows) {
            val y = rowStep * j
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = strokePx
            )
        }
    }
}
