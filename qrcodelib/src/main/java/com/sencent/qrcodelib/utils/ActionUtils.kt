package com.sencent.qrcodelib.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore

/**
 *  Create by Logan at 2018/12/17 0017
 *
 */
object ActionUtils {

    private val TAG = ActionUtils::class.java.simpleName

    val PHOTO_REQUEST_GALLERY = 1000
    val PHOTO_REQUEST_CAMERA = 1001
    val PHOTO_REQUEST_CUT = 1002

    /**
     * 进入系统拍照
     * @param activity
     * @param outputUri 照片输出路径 Environment.getExternalStorageDirectory() + "/image.jpg")
     */
    fun startActivityForCamera(activity: Activity, requestCode: Int, outputUri: Uri) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        // 制定图片保存路径
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * 进入系统拍照 (输出为Bitmap)
     * Tips: 返回的Bitmap并非原图的Bitmap而是经过压缩的Bitmap
     * @param activity
     */
    fun startActivityForCamera(activity: Activity, requestCode: Int) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * 进入系统图库
     * @param activity
     */
    fun startActivityForGallery(activity: Activity, requestCode: Int) {
        // 弹出系统图库
        val i = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        activity.startActivityForResult(i, requestCode)
    }

    /**
     * 进入系统裁剪
     * @param inputUri 需要裁剪的图片路径
     * @param outputUri 裁剪后图片路径 Environment.getExternalStorageDirectory() + "/image.jpg")
     * @param width 裁剪后宽度(px)
     * @param height 裁剪后高度(px)
     */
    private fun startActivityForImageCut(
        activity: Activity, requestCode: Int,
        inputUri: Uri, outputUri: Uri,
        width: Int, height: Int
    ) {
        val intent = Intent("com.android.camera.action.CROP")
        intent.setDataAndType(inputUri, "image/*")
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true")
        intent.putExtra("scale", true) // 去黑边
        intent.putExtra("scaleUpIfNeeded", true) // 去黑边
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1) // 输出是X方向的比例
        intent.putExtra("aspectY", 1)
        // outputX outputY 是裁剪图片宽高，切忌不要再改动下列数字，会卡死
        intent.putExtra("outputX", width) // 输出X方向的像素
        intent.putExtra("outputY", height)
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
        intent.putExtra("noFaceDetection", true)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        intent.putExtra("return-data", false) // 设置为不返回数据
        activity.startActivityForResult(intent, requestCode)
    }
}