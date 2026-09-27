package com.ward.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.ui.compose.GlassSurface
import com.v2ray.ang.ui.compose.LocalContentBackdrop
import com.v2ray.ang.ui.compose.LocalDarkTheme

/** Стеклянная таблетка с подписью - кнопки под кнопкой питания и в шапках карточек. */
@Composable
fun GlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary
) {
    GlassSurface(
        modifier = modifier.clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        backdrop = LocalContentBackdrop.current,
        opaqueness = 0.4f,
        surfaceTint = color.copy(alpha = if (LocalDarkTheme.current) 0.14f else 0.10f),
        dispersion = false
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (enabled) color else color.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024; i++
    }
    return if (i == 0) "$bytes B" else String.format("%.1f %s", v, units[i])
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
}
