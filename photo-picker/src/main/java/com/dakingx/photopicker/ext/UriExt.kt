package com.dakingx.photopicker.ext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

fun Uri.toBitmap(context: Context): Bitmap {
    val fd = context.contentResolver.openFileDescriptor(this, "r")
        ?: throw IllegalStateException("open fd fail, uri is $this.")
    return fd.use {
        BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
    }
}
