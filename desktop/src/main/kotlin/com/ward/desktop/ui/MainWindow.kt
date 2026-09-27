package com.ward.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.ui.compose.GlassSurface
import com.v2ray.ang.ui.compose.LiquidPowerButton
import com.v2ray.ang.ui.compose.LocalContentBackdrop
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.LocalServiceColors
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingRed
import com.v2ray.ang.ui.compose.colorPingSlow
import com.v2ray.ang.ui.compose.liquidBackground
import com.v2ray.ang.ui.compose.rememberGlassBackdrop
import com.ward.desktop.core.AppController
import com.ward.desktop.core.ConnectionState
import com.ward.desktop.core.ServerEntry
import com.ward.desktop.core.ServerGroup
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(26.dp)

@Composable
fun MainScreen(controller: AppController, onOpenSettings: () -> Unit) {
    val state by controller.state.collectAsState()
    val groups by controller.groups.collectAsState()
    val selected by controller.selected.collectAsState()
    val busy by controller.busy.collectAsState()
    val messages by controller.messages.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    val contentBackdrop = rememberGlassBackdrop()
    val activity = when (state) {
        is ConnectionState.Connected -> 1f
        ConnectionState.Connecting -> 0.5f
        ConnectionState.Disconnected -> 0f
    }

    CompositionLocalProvider(LocalContentBackdrop provides contentBackdrop) {
        Box(
            Modifier.fillMaxSize().liquidBackground(contentBackdrop) { activity }
        ) {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { PowerSection(state, onToggle = controller::toggle) }
                item {
                    Row(
                        Modifier.padding(top = 18.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassPill("Добавить", onClick = { showAdd = true })
                        GlassPill("Обновить", onClick = controller::updateAllSubscriptions, enabled = busy == null)
                        GlassPill("Проверить", onClick = { controller.testServers() }, enabled = busy == null)
                        if (state is ConnectionState.Connected) {
                            GlassPill("Связь", onClick = controller::testConnection)
                        }
                        GlassPill("⚙", onClick = onOpenSettings)
                    }
                }
                if (busy != null) {
                    item {
                        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(bottom = 12.dp)) {
                            Text(busy!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape))
                        }
                    }
                }
                if (groups.isEmpty()) {
                    item { EmptyHint(onAdd = { showAdd = true }) }
                }
                groups.forEach { group ->
                    item(key = "g-" + group.id) {
                        GroupCard(
                            group = group,
                            onUpdate = { controller.updateSubscription(group.id) },
                            onTest = { controller.testServers(group.id) },
                            onDelete = { controller.deleteGroup(group.id) }
                        )
                    }
                    items(group.servers, key = { "s-" + group.id + it.guid }) { server ->
                        ServerRow(server, server.guid == selected) { controller.select(server.guid) }
                    }
                    item(key = "gap-" + group.id) { Spacer(Modifier.height(14.dp)) }
                }
            }

            MessageBar(
                message = messages.firstOrNull(),
                onDismiss = { controller.consumeMessage(it) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
    }

    if (showAdd) {
        AddDialog(onDismiss = { showAdd = false }, onAdd = {
            showAdd = false
            controller.import(it)
        })
    }
}

@Composable
private fun PowerSection(state: ConnectionState, onToggle: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) {
        while (state is ConnectionState.Connected) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val connected = state is ConnectionState.Connected
    LiquidPowerButton(
        isConnected = connected,
        isConnecting = state == ConnectionState.Connecting,
        statusText = when (state) {
            is ConnectionState.Connected -> "ПОДКЛЮЧЕН"
            ConnectionState.Connecting -> "ПОДКЛЮЧЕНИЕ"
            ConnectionState.Disconnected -> "ОТКЛЮЧЕН"
        },
        timeString = if (state is ConnectionState.Connected) formatDuration(now - state.since) else "",
        onClick = onToggle
    )
}

@Composable
private fun EmptyHint(onAdd: () -> Unit) {
    Column(
        Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Серверов пока нет", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Добавьте ссылку на подписку или ключ vless://, vmess://, trojan://, ss://",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        GlassPill("Добавить", onClick = onAdd)
    }
}

@Composable
private fun GroupCard(group: ServerGroup, onUpdate: () -> Unit, onTest: () -> Unit, onDelete: () -> Unit) {
    val sub = group.subscription?.subscription
    val tint = if (LocalServiceColors.current) serviceColor(sub?.color.orEmpty(), LocalDarkTheme.current) else null
    var menu by remember { mutableStateOf(false) }

    GlassSurface(
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(bottom = 8.dp),
        shape = CardShape,
        backdrop = LocalContentBackdrop.current,
        opaqueness = 0.35f,
        surfaceTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (LocalDarkTheme.current) 0.52f else 0.58f),
        dispersion = false,
        fallbackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        border = tint?.copy(alpha = 0.55f),
        innerGlow = tint?.copy(alpha = 0.30f),
        innerGlowDepth = 9.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(group.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val status = when {
                        sub == null -> "Серверов: ${group.servers.size}"
                        sub.lastUpdated > 0 -> "Обновлено " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(sub.lastUpdated))
                        else -> "Ещё не обновлялась"
                    }
                    Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    Text(
                        "⋮",
                        fontSize = 20.sp,
                        modifier = Modifier.clip(CircleShape).clickable { menu = true }.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (sub != null) {
                            DropdownMenuItem(text = { Text("Обновить") }, onClick = { menu = false; onUpdate() })
                        }
                        DropdownMenuItem(text = { Text("Проверить серверы") }, onClick = { menu = false; onTest() })
                        DropdownMenuItem(
                            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }

            if (sub != null && (sub.trafficTotal > 0 || sub.trafficUpload + sub.trafficDownload > 0 || sub.trafficExpire > 0)) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val used = sub.trafficUpload + sub.trafficDownload
                    val total = if (sub.trafficTotal > 0) formatBytes(sub.trafficTotal) else "∞"
                    Box(
                        Modifier.clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("${formatBytes(used)}/$total", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (sub.trafficExpire > 0) {
                        Text(
                            "Истекает: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(sub.trafficExpire * 1000)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (!sub?.announce.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    sub!!.announce,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ServerRow(server: ServerEntry, isSelected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(vertical = 2.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) scheme.primary.copy(alpha = 0.14f) else scheme.surface.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(4.dp).height(28.dp).clip(CircleShape)
                .background(if (isSelected) scheme.primary else Color.Transparent)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(server.profile.remarks, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val desc = server.profile.serverDescription?.takeIf { it.isNotBlank() }
                ?: server.profile.configType.name.lowercase()
            Text(desc, fontSize = 11.sp, lineHeight = 14.sp, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val d = server.delayMillis
        if (d != 0L) {
            val c = when {
                d < 0 -> colorPingRed
                d > 500 -> colorPingSlow
                else -> colorPing
            }
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(c.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(if (d < 0) "нет" else "$d ms", color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MessageBar(message: String?, onDismiss: (String) -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(4000)
            onDismiss(message)
        }
    }
    AnimatedVisibility(visible = message != null, modifier = modifier) {
        val text = message ?: return@AnimatedVisibility
        GlassSurface(
            modifier = Modifier.widthIn(max = 520.dp).clickable { onDismiss(text) },
            shape = RoundedCornerShape(20.dp),
            backdrop = LocalContentBackdrop.current,
            opaqueness = 0.8f,
            dispersion = false
        ) {
            Text(text, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), fontSize = 13.sp)
        }
    }
}
