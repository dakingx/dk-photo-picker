package com.dakingx.photopicker.util

import androidx.fragment.app.FragmentManager
import com.dakingx.photopicker.fragment.PhotoFragment
import com.dakingx.photopicker.fragment.PhotoOpCallback
import com.dakingx.photopicker.fragment.PhotoOpResult

import android.net.Uri
import com.dakingx.photopicker.ext.resumeSafely
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

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
private fun genCropPhotoCb(
    fragment: PhotoFragment,
    continuation: CancellableContinuation<PhotoOpResult>
) =
    object : PhotoOpCallback {
        override fun invoke(result: PhotoOpResult) {
            when (result) {
                is PhotoOpResult.Success -> {
                    // 裁剪
                    fragment.crop(result.uri) { cropResult ->
                        continuation.resumeSafely(cropResult)
                    }
                }
                else -> {
                    continuation.resumeSafely(result)
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
        continuation.resumeSafely(cropResult)
    }
}

/**
 * 获取PhotoFragment
 */
private fun getPhotoFragment(fm: FragmentManager, fileProviderAuthority: String) =
    fm.findFragmentByTag(PhotoFragment.FRAGMENT_TAG) as? PhotoFragment
        ?: PhotoFragment.newInstance(fileProviderAuthority).apply {
            fm.beginTransaction()
                .add(this, PhotoFragment.FRAGMENT_TAG)
                .commitAllowingStateLoss()
            fm.executePendingTransactions()
        }
