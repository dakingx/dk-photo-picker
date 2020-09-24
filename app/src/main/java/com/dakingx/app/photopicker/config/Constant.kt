package com.dakingx.app.photopicker.config

import android.content.Context

fun getFileProviderAuthority(context: Context) = "${context.packageName}.FILE_PROVIDER"