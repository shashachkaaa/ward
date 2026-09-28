package com.v2ray.ang.helper

import android.net.Uri
import androidx.activity.ComponentActivity
import com.ward.desktop.FileDialogs

/** Выбор и создание файла - системными диалогами вместо Storage Access Framework. */
class FileChooserHelper(private val activity: ComponentActivity) {

    fun launch(mimeType: String = "*/*", onResult: (Uri?) -> Unit) {
        onResult(FileDialogs.open(mimeType)?.let(Uri::fromFile))
    }

    fun createDocument(fileName: String, onResult: (Uri?) -> Unit) {
        onResult(FileDialogs.save(fileName)?.let(Uri::fromFile))
    }
}
