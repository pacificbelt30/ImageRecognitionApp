package com.example.cameras.utils

import androidx.activity.ComponentActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.cameras.R

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
        File(it, ca.resources.getString(R.string.app_name)).apply { mkdirs() }
    }
    return if (outDir != null && outDir.exists()) outDir else ca.filesDir
}
