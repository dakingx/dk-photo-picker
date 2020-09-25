package com.dakingx.photopicker.fragment

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.dakingx.photopicker.ext.checkAppPermission
import com.dakingx.photopicker.ext.filePath2Uri
import com.dakingx.photopicker.ext.generateTempFile
import java.lang.RuntimeException

sealed class PhotoOpResult {
    class Success(val uri: Uri) : PhotoOpResult()

    object Failure : PhotoOpResult()

    object Cancel : PhotoOpResult()
}

typealias PhotoOpCallback = (PhotoOpResult) -> Unit

class PhotoFragment : BaseFragment() {

    companion object {
        const val FRAGMENT_TAG = "photo_fragment"

        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        @JvmStatic
        fun newInstance(fileProviderAuthority: String) =
            PhotoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILE_PROVIDER_AUTH, fileProviderAuthority)
                }
            }

        private const val ARG_FILE_PROVIDER_AUTH = "arg_file_provider_auth"

        private const val REQ_CODE_CAPTURE = 0x601
        private const val REQ_CODE_PICK = 0x602
        private const val REQ_CODE_CROP = 0x603
    }

    private var fileProviderAuthority: String = ""

    private lateinit var captureFilePath: String
    private lateinit var cropFilePath: String

    private var captureCallback: PhotoOpCallback? = null
    private var pickCallback: PhotoOpCallback? = null
    private var cropCallback: PhotoOpCallback? = null

    override fun restoreState(bundle: Bundle?) {
        bundle?.let {
            bundle.getString(ARG_FILE_PROVIDER_AUTH)?.let {
                fileProviderAuthority = it
            }
        }
    }

    override fun storeState(bundle: Bundle) {
        bundle.also {
            it.putString(ARG_FILE_PROVIDER_AUTH, fileProviderAuthority)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        if (fileProviderAuthority.isEmpty()) {
            throw RuntimeException("fileProviderAuthority can't be empty")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        storeState(outState)
    }

    override fun onDestroy() {
        captureCallback?.invoke(PhotoOpResult.Cancel)
        pickCallback?.invoke(PhotoOpResult.Cancel)
        cropCallback?.invoke(PhotoOpResult.Cancel)

        super.onDestroy()
    }

    fun capture(callback: PhotoOpCallback) {
        // 应用权限检查
        if (!checkRequiredPermissions()) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }

        this.captureCallback = callback

        val file = context?.generateTempFile("capture_photo")
        if (file == null) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }
        this.captureFilePath = file.absolutePath

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(requireContext(), fileProviderAuthority, file)
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        startActivityForResult(intent, REQ_CODE_CAPTURE)
    }

    fun pick(callback: PhotoOpCallback) {
        // 应用权限检查
        if (!checkRequiredPermissions()) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }

        this.pickCallback = callback

        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, REQ_CODE_PICK)
    }

    fun crop(uri: Uri, callback: PhotoOpCallback) {
        // 应用权限检查
        if (!checkRequiredPermissions()) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }

        val cropIntent = Intent("com.android.camera.action.CROP")

        this.cropCallback = callback

        val sourceUri =
            if (uri.scheme.equals(ContentResolver.SCHEME_FILE)) FileProvider.getUriForFile(
                requireContext(), fileProviderAuthority, uri.toFile()
            )
            else uri

        val file = context?.generateTempFile("crop_photo")
        if (file == null) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }
        this.cropFilePath = file.absolutePath
        val destinationUri = Uri.fromFile(file)

        cropIntent.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(sourceUri, requireContext().contentResolver.getType(sourceUri))
            putExtra("noFaceDetection", true)
            putExtra("crop", "true")
            putExtra("scale", true)
            putExtra("scaleUpIfNeeded", true)
            putExtra(MediaStore.EXTRA_OUTPUT, destinationUri)
            putExtra("outputFormat", Bitmap.CompressFormat.JPEG.name)
            putExtra("return-data", false)
        }
        startActivityForResult(cropIntent, REQ_CODE_CROP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_CODE_CAPTURE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val uri = filePath2Uri(captureFilePath)

                        captureCallback?.invoke(
                            if (uri != null) PhotoOpResult.Success(uri)
                            else PhotoOpResult.Failure
                        )
                        captureCallback = null
                    }
                    Activity.RESULT_CANCELED -> {
                        captureCallback?.invoke(PhotoOpResult.Cancel)
                        captureCallback = null
                    }
                    else -> {
                        captureCallback?.invoke(PhotoOpResult.Failure)
                        captureCallback = null
                    }
                }
            }
            REQ_CODE_PICK -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val uri = data?.data

                        pickCallback?.invoke(
                            if (uri != null) PhotoOpResult.Success(uri)
                            else PhotoOpResult.Failure
                        )
                        pickCallback = null
                    }
                    Activity.RESULT_CANCELED -> {
                        pickCallback?.invoke(PhotoOpResult.Cancel)
                        pickCallback = null
                    }
                    else -> {
                        pickCallback?.invoke(PhotoOpResult.Failure)
                        pickCallback = null
                    }
                }
            }
            REQ_CODE_CROP -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val uri = filePath2Uri(cropFilePath)

                        cropCallback?.invoke(
                            if (uri != null) PhotoOpResult.Success(uri)
                            else PhotoOpResult.Failure
                        )
                        cropCallback = null
                    }
                    Activity.RESULT_CANCELED -> {
                        cropCallback?.invoke(PhotoOpResult.Cancel)
                        cropCallback = null
                    }
                    else -> {
                        cropCallback?.invoke(PhotoOpResult.Failure)
                        cropCallback = null
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun filePath2Uri(filePath: String): Uri? =
        context?.filePath2Uri(fileProviderAuthority, filePath)

    private fun checkRequiredPermissions(): Boolean =
        context?.checkAppPermission(*REQUIRED_PERMISSIONS.toTypedArray()) ?: false
}
