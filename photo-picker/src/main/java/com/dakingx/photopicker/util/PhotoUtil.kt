package com.dakingx.photopicker.util

import androidx.fragment.app.FragmentManager
import com.dakingx.photopicker.fragment.PhotoFragment
import com.dakingx.photopicker.fragment.PhotoOpCallback
import com.dakingx.photopicker.fragment.PhotoOpResult

import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation

/**
 * 拍照并裁剪
 */
suspend fun capturePhoto(fm: FragmentManager, authority: String): PhotoOpResult =
    suspendCancellableCoroutine { continuation ->
        val fragment = getPhotoFragment(fm, authority)
        fragment.capture(
            genCropPhotoCb(fragment, continuation)
        )
    }

/**
 * 选取照片并裁剪
 */
suspend fun pickPhoto(fm: FragmentManager, authority: String): PhotoOpResult =
    suspendCancellableCoroutine { continuation ->
        val fragment = getPhotoFragment(fm, authority)
        fragment.pick(
            genCropPhotoCb(fragment, continuation)
        )
    }

/**
 * 处理裁剪
 */
private fun genCropPhotoCb(fragment: PhotoFragment, continuation: Continuation<PhotoOpResult>) =
    object : PhotoOpCallback {
        override fun invoke(result: PhotoOpResult) {
            when (result) {
                is PhotoOpResult.Success -> {
                    // 裁剪
                    fragment.crop(result.uri) { cropResult ->
                        continuation.resumeWith(Result.success(cropResult))
                    }
                }
                PhotoOpResult.Failure -> {
                    continuation.resumeWith(Result.success(result))
                }
            }
        }
    }

/**
 * 裁剪
 */
suspend fun cropPhoto(
    fm: FragmentManager,
    authority: String,
    sourceUri: Uri
): PhotoOpResult = suspendCancellableCoroutine { continuation ->
    val fragment = getPhotoFragment(fm, authority)
    fragment.crop(sourceUri) { cropResult ->
        continuation.resumeWith(Result.success(cropResult))
    }
}

/**
 * 获取PhotoFragment
 */
private fun getPhotoFragment(fm: FragmentManager, fileProviderAuthority: String) =
    fm.findFragmentByTag(PhotoFragment.FRAGMENT_TAG) as? PhotoFragment ?: PhotoFragment().apply {
        // fragment参数
        val bundle = Bundle()
        bundle.putString(PhotoFragment.ARG_FILE_PROVIDER_AUTH, fileProviderAuthority)
        arguments = bundle

        fm.beginTransaction().add(this, PhotoFragment.FRAGMENT_TAG).commitAllowingStateLoss()
        fm.executePendingTransactions()
    }
