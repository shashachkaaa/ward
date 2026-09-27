package com.ward.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.v2ray.ang.util.Utils
import android.content.Context

/** Добавление: ссылка на подписку, один ключ или несколько ключей построчно. */
@Composable
fun AddDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    // Буфер обмена - самый частый источник: ссылку копируют с сайта сервиса
    var text by remember {
        val clip = Utils.getClipboard(Context.app).trim()
        mutableStateOf(if (looksImportable(clip)) clip else "")
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(Modifier.width(460.dp).padding(24.dp)) {
                Text("Добавить", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Ссылка на подписку или ключи vless://, vmess://, trojan://, ss://, hysteria2://",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = { Text("https://…") }
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { text = Utils.getClipboard(Context.app).trim() }) { Text("Вставить") }
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text("Добавить") }
                }
            }
        }
    }
}

private fun looksImportable(s: String): Boolean =
    s.startsWith("http://") || s.startsWith("https://") || s.contains("://") && s.length < 20_000
