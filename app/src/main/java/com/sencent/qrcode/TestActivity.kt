package com.sencent.qrcode

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.sencent.qrcodelib.CaptureView
import com.sencent.qrcodelib.ScanListener
import com.sencent.qrcodelib.camera.FrontLightMode
import com.sencent.qrcodelib.utils.ActionUtils

/**
 *  Create by Logan at 2018/12/17 0017
 *
 */
class TestActivity : AppCompatActivity(), ScanListener {
    override fun onFinish() {
        finish()
    }

    override fun onSuccess(result: String) {
        Toast.makeText(this@TestActivity, result, Toast.LENGTH_SHORT).show()
        captureView?.restartPreviewAfterDelay(300L)
    }

    override fun onError(errorCode: Int) {
        when (errorCode) {
            CaptureView.CAMERA_ERROR -> AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("相机加载异常，无法正常使用")
                .setPositiveButton("确定") { _, _ -> finish() }
                .show()
            CaptureView.BEEP_MANAGER_ERROR -> {

            }
            else -> finish()
        }
    }

    private var captureView: CaptureView?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        captureView = findViewById(R.id.cv_qrcode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_PERMISSION_CAMERA
                )
            }
        }
        captureView?.setPlayBeep(true)
        captureView?.setVibrate(false)
        captureView?.setScanListener(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // 未获得Camera权限
                AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("请在系统设置中为App开启摄像头权限后重试")
                    .setPositiveButton("确定") { dialog, which -> this.finish() }
                    .show()
            }
        } else if (grantResults.isNotEmpty() && requestCode == REQUEST_PERMISSION_PHOTO) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("请在系统设置中为App中开启文件权限后重试")
                    .setPositiveButton("确定", null)
                    .show()
            } else {
                ActionUtils.startActivityForGallery(this, ActionUtils.PHOTO_REQUEST_GALLERY)
            }
        }
    }

    override fun onResume() {
        captureView?.onResume()
        super.onResume()
    }

    override fun onPause() {
        captureView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        captureView?.onDestroy()
        super.onDestroy()
    }



    private fun clickFlash(v: View) {
        if (v.isSelected) {
          captureView?.flashOff()
            v.isSelected = false
        } else {
           captureView?.flashOn()
            v.isSelected = true
        }

    }

    fun OnClick(v: View) {
        when (v.id) {
            R.id.ivLeft -> onBackPressed()
            R.id.ivFlash -> clickFlash(v)
            R.id.openAlbum -> {
                openGallery()
            }
        }
    }

    /**
     * 打开相册
     */
    private fun openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_PHOTO
            )
        } else {
            ActionUtils.startActivityForGallery(this, ActionUtils.PHOTO_REQUEST_GALLERY)
        }
    }


    companion object {
        private val REQUEST_PERMISSION_CAMERA = 1000
        private val REQUEST_PERMISSION_PHOTO = 1001
    }
}