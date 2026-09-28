package com.ward.desktop

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Системные диалоги открытия и сохранения файла - замена выбору документа на Android. */
object FileDialogs {
    /** Окно приложения: диалог встаёт поверх него. Выставляет Main. */
    var owner: Frame? = null

    fun open(mimeType: String = "*/*"): File? {
        val d = FileDialog(owner, null as String?, FileDialog.LOAD)
        extensionFor(mimeType)?.let { ext -> d.setFilenameFilter { _, name -> name.endsWith(ext, ignoreCase = true) } }
        d.isVisible = true
        return d.files.firstOrNull()
    }

    fun save(suggestedName: String): File? {
        val d = FileDialog(owner, null as String?, FileDialog.SAVE)
        d.file = suggestedName
        d.isVisible = true
        val dir = d.directory ?: return null
        val name = d.file ?: return null
        return File(dir, name)
    }

    private fun extensionFor(mime: String): String? = when (mime) {
        "application/zip" -> ".zip"
        "application/json" -> ".json"
        else -> null
    }
}
