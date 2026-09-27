package com.ward.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.handler.MmkvManager
import com.ward.desktop.DesktopPaths
import com.ward.desktop.Platform

/**
 * Настройки первой версии - только то, без чего не обойтись: порт, доступ из
 * локальной сети и журнал. Полный раздел настроек придёт вместе с туннелем.
 */
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    var port by remember { mutableStateOf(MmkvManager.decodeSettingsString(AppConfig.PREF_SOCKS_PORT) ?: AppConfig.PORT_SOCKS) }
    var sharing by remember { mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)) }
    var serviceColors by remember { mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_SERVICE_COLORS, true)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(Modifier.width(460.dp).padding(24.dp)) {
                Text("Настройки", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Изменения вступят в силу при следующем подключении",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { v -> port = v.filter(Char::isDigit).take(5) },
                    label = { Text("Порт SOCKS (HTTP - следующий за ним)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SwitchRow("Пускать устройства из локальной сети", sharing) { sharing = it }
                SwitchRow("Цвета сервисов на карточках", serviceColors) { serviceColors = it }

                Text(
                    "Ward ${BuildConfig.VERSION_NAME} · данные в ${DesktopPaths.dataDir}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { Platform.openFolder(DesktopPaths.logDir) }) { Text("Журнал") }
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = {
                        val p = port.toIntOrNull()?.takeIf { it in 1024..65534 }
                        if (p != null) MmkvManager.encodeSettings(AppConfig.PREF_SOCKS_PORT, p.toString())
                        MmkvManager.encodeSettings(AppConfig.PREF_PROXY_SHARING, sharing)
                        MmkvManager.encodeSettings(AppConfig.PREF_SERVICE_COLORS, serviceColors)
                        onDismiss()
                    }) { Text("Сохранить") }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
