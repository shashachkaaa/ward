package com.v2ray.ang.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// Скопировано из MainScreen.kt приложения под Android

@Composable
fun PowerIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Толщина от размера, а не в пикселях: на плотных экранах фиксированные
        // 5 пикселей превращались в волосок
        val strokeW = size.minDimension * 0.1f
        val side = size.minDimension - strokeW
        drawArc(
            color = color,
            startAngle = -240f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(strokeW / 2f, strokeW / 2f),
            size = Size(side, side),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = center.copy(y = strokeW / 2f),
            end = center.copy(y = center.y),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}
