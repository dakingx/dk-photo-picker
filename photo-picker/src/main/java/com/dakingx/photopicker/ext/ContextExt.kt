package com.dakingx.photopicker.ext

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import kotlin.random.Random

fun Context.generateTempFile(prefix: String, extension: String = "jpg"): File? {
    val fileName = "${prefix}_${System.currentTimeMillis()}_${Random.nextInt(9999)}.${extension}"
    val extCacheDir = this.externalCacheDir
    return if (extCacheDir != null && Environment.isExternalStorageEmulated(extCacheDir)) {
        File(extCacheDir.absolutePath, fileName)
    } else {
        null
    }
}

fun Context.filePath2Uri(fileProviderAuthority: String, filePath: String): Uri? =
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FileProvider.getUriForFile(this, fileProviderAuthority, File(filePath))
        } else {
            Uri.fromFile(File(filePath))
        }
    } catch (e: Throwable) {
        null
    }

fun Context.checkAppPermission(vararg permission: String): Boolean = permission.all {
    ContextCompat.checkSelfPermission(
        this, it
    ) == PackageManager.PERMISSION_GRANTED
}
