package com.example.cameras.utils

import androidx.activity.ComponentActivity
import java.io.File
import java.text.SimpleDateFormat

/**
 * Creates a photo file with timestamp-based name
 */
public fun createPhotoFile(outputDirectory: File): File {
    val filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS"
    val timestamp = SimpleDateFormat(filenameFormat, Locale.JAPAN).format(System.currentTimeMillis())
    return File(outputDirectory, "$timestamp.jpg")
}

/**
  * 保存先ディレクトリを取得
  */
public fun getOutputDirectory(ca: ComponentActivity): File {
    // Scoped storage(対象範囲別ストレージ)
    val outDir = ca.getExternalFilesDir(null)?.path.let {
        File(ca, ca.resources.getString(ca.R.string.app_name)).apply { ca.mkdirs() }
    }
    return if (outDir != null && outDir.exists()) outDir else ca.filesDir
}
