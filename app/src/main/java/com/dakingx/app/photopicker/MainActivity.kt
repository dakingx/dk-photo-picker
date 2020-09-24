package com.dakingx.app.photopicker

import android.Manifest
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.dakingx.app.photopicker.config.getFileProviderAuthority
import com.dakingx.photopicker.ext.toBitmap
import com.dakingx.photopicker.fragment.PhotoOpResult
import com.dakingx.photopicker.util.capturePhoto
import com.dakingx.photopicker.util.pickPhoto
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var coroutineScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        coroutineScope = MainScope()

        captureBtn.setOnClickListener {
            capture()
        }

        galleryBtn.setOnClickListener {
            pickFromGallery()
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()

        super.onDestroy()
    }

    private fun toast(stringResId: Int) {
        Toast.makeText(this, getString(stringResId), Toast.LENGTH_SHORT).show()
    }

    private fun capture() =
        requestPermission(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) {
            captureActual()
        }

    private fun captureActual() {
        coroutineScope.launch {
            when (val result =
                capturePhoto(supportFragmentManager, getFileProviderAuthority(this@MainActivity))) {
                is PhotoOpResult.Success -> handlePhotoUri(result.uri)
                PhotoOpResult.Failure -> toast(R.string.main_tip_op_fail)
                PhotoOpResult.Cancel -> toast(R.string.main_tip_op_cancel)
            }
        }
    }

    private fun pickFromGallery() =
        requestPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) {
            pickFromGalleryActual()
        }

    private fun pickFromGalleryActual() {
        coroutineScope.launch {
            when (val result =
                pickPhoto(supportFragmentManager, getFileProviderAuthority(this@MainActivity))) {
                is PhotoOpResult.Success -> handlePhotoUri(result.uri)
                PhotoOpResult.Failure -> toast(R.string.main_tip_op_fail)
                PhotoOpResult.Cancel -> toast(R.string.main_tip_op_cancel)
            }
        }
    }

    private fun handlePhotoUri(uri: Uri) {
        val bitmap = uri.toBitmap(this)
        photoIv.setImageBitmap(bitmap)
    }

    private fun requestPermission(vararg permission: String, successAction: () -> Unit) {
        Dexter.withContext(this)
            .withPermissions(*permission).withListener(object :
                MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        runOnUiThread {
                            successAction()
                        }
                    } else {
                        runOnUiThread {
                            toast(R.string.main_tip_lost_permission)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    list: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }
}
