package com.dakingx.photopicker.fragment

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.dakingx.photopicker.ext.checkAppPermission
import com.dakingx.photopicker.ext.filePath2Uri
import com.dakingx.photopicker.ext.generateTempFile
import com.dakingx.photopicker.ext.generateTempFile2
import java.lang.RuntimeException
import kotlin.random.Random

sealed class PhotoOpResult {
    class Success(val uri: Uri) : PhotoOpResult()

    object Failure : PhotoOpResult()

    object Cancel : PhotoOpResult()
}

typealias PhotoOpCallback = (PhotoOpResult) -> Unit

class PhotoFragment : BaseFragment() {

    companion object {
        const val FRAGMENT_TAG = "photo_fragment"

        val REQUIRED_PERMISSIONS_FOR_CAPTURE = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val REQUIRED_PERMISSIONS_FOR_PICK = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val REQUIRED_PERMISSIONS_FOR_CROP = listOf(
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

    private var captureFileUri: Uri? = null
    private var cropFileUri: Uri? = null

    private var captureCallback: PhotoOpCallback? = null
    private var pickCallback: PhotoOpCallback? = null
    private var cropCallback: PhotoOpCallback? = null

    override fun restoreState(bundle: Bundle?) {
        bundle?.apply {
            getString(ARG_FILE_PROVIDER_AUTH)?.let {
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

    override fun onDestroy() {
        captureCallback?.invoke(PhotoOpResult.Cancel)
        captureCallback = null
        pickCallback?.invoke(PhotoOpResult.Cancel)
        pickCallback = null
        cropCallback?.invoke(PhotoOpResult.Cancel)
        cropCallback = null

        super.onDestroy()
    }

    fun capture(callback: PhotoOpCallback) {
        // 应用权限检查
        if (!checkRequiredPermissions(REQUIRED_PERMISSIONS_FOR_CAPTURE)) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }

        this.captureCallback = callback

        val file = context?.generateTempFile("capture_photo")
        if (file == null) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(requireContext(), fileProviderAuthority, file)
        } else {
            Uri.fromFile(file)
        }

        captureFileUri = uri

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        startActivityForResult(intent, REQ_CODE_CAPTURE)
    }

    fun pick(callback: PhotoOpCallback) {
        // 应用权限检查
        if (!checkRequiredPermissions(REQUIRED_PERMISSIONS_FOR_PICK)) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }

        this.pickCallback = callback

        val intent =
            Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
        startActivityForResult(intent, REQ_CODE_PICK)
    }

    fun crop(uri: Uri, callback: PhotoOpCallback) {
        // 应用权限检查
        if (!checkRequiredPermissions(REQUIRED_PERMISSIONS_FOR_CROP)) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }

        val ctx = requireContext()

        val cropIntent = Intent("com.android.camera.action.CROP")

        this.cropCallback = callback

        val sourceUri =
            if (uri.scheme.equals(ContentResolver.SCHEME_FILE)) FileProvider.getUriForFile(
                requireContext(), fileProviderAuthority, uri.toFile()
            )
            else uri

        val mimeType = ctx.contentResolver.getType(sourceUri)
        val fileName = "crop_photo_${System.currentTimeMillis()}_${Random.nextInt(9999)}.jpg"
        val destinationUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
        } else {
            val file = context?.generateTempFile2(fileName)
            if (file == null) {
                callback.invoke(PhotoOpResult.Failure)
                return
            }
            Uri.fromFile(file)
        }
        if (destinationUri == null) {
            callback.invoke(PhotoOpResult.Failure)
            return
        }
        cropFileUri = destinationUri

        cropIntent.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            setDataAndType(sourceUri, mimeType)
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
                        val uri = captureFileUri

                        captureCallback?.invoke(
                            if (uri != null) PhotoOpResult.Success(uri)
                            else PhotoOpResult.Failure
                        )
                    }
                    Activity.RESULT_CANCELED -> {
                        captureCallback?.invoke(PhotoOpResult.Cancel)
                    }
                    else -> {
                        captureCallback?.invoke(PhotoOpResult.Failure)
                    }
                }
                captureCallback = null
                captureFileUri = null
            }
            REQ_CODE_PICK -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val uri = data?.data

                        pickCallback?.invoke(
                            if (uri != null) PhotoOpResult.Success(uri)
                            else PhotoOpResult.Failure
                        )
                    }
                    Activity.RESULT_CANCELED -> {
                        pickCallback?.invoke(PhotoOpResult.Cancel)
                    }
                    else -> {
                        pickCallback?.invoke(PhotoOpResult.Failure)
                    }
                }
                pickCallback = null
            }
            REQ_CODE_CROP -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val uri = cropFileUri

                        cropCallback?.invoke(
                            if (uri != null) PhotoOpResult.Success(uri)
                            else PhotoOpResult.Failure
                        )
                    }
                    Activity.RESULT_CANCELED -> {
                        cropCallback?.invoke(PhotoOpResult.Cancel)
                    }
                    else -> {
                        cropCallback?.invoke(PhotoOpResult.Failure)
                    }
                }
                cropCallback = null
                cropFileUri = null
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun filePath2Uri(filePath: String): Uri? =
        context?.filePath2Uri(fileProviderAuthority, filePath)

    private fun checkRequiredPermissions(requiredPermissions: List<String>): Boolean =
        context?.checkAppPermission(*requiredPermissions.toTypedArray()) ?: false
}
