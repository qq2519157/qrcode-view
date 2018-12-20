package com.sencent.qrcodelib.camera.open

import android.hardware.Camera

/**
 *  Create by Logan at 2018/12/14 0014
 *  相机的包装类，包含相机id  相机实例  前置/后置  方向
 */
class OpenCamera(private val index: Int, val camera: Camera, val facing: CameraFacing, val orientation: Int) {

    override fun toString(): String {
        return "Camera #" + index + " : " + facing + ','.toString() + orientation
    }


}