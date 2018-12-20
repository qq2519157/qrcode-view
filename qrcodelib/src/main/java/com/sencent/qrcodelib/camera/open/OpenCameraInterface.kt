package com.sencent.qrcodelib.camera.open

import android.hardware.Camera
import android.util.Log

/**
 *  Create by Logan at 2018/12/14 0014
 *  提供静态方法实例化OpenCamera
 */
object OpenCameraInterface {

    private val TAG = OpenCameraInterface::class.java.name

    /**
     * For [.open], means no preference for which camera to open.
     */
    val NO_REQUESTED_CAMERA = -1

    /**
     * Opens the requested camera with [Camera.open], if one exists.
     *
     * @param cameraId camera ID of the camera to use. A negative value
     * or [.NO_REQUESTED_CAMERA] means "no preference", in which case a rear-facing
     * camera is returned if possible or else any camera
     * @return handle to [OpenCamera] that was opened
     */
    fun open(cameraId: Int): OpenCamera? {
        var cameraId = cameraId
        val numCameras = Camera.getNumberOfCameras()
        if (numCameras == 0) {
            Log.w(TAG, "No cameras!")
            return null
        }
        if (cameraId >= numCameras) {
            Log.w(TAG, "Requested camera does not exist: $cameraId")
            return null
        }
        if (cameraId <= NO_REQUESTED_CAMERA) {
            cameraId = 0
            while (cameraId < numCameras) {
                val cameraInfo = Camera.CameraInfo()
                Camera.getCameraInfo(cameraId, cameraInfo)
                if (CameraFacing.values()[cameraInfo.facing] === CameraFacing.BACK) {
                    break
                }
                cameraId++
            }
            if (cameraId == numCameras) {
                Log.i(TAG, "No camera facing " + CameraFacing.BACK + "; returning camera #0")
                cameraId = 0
            }
        }
        Log.i(TAG, "Opening camera #$cameraId")
        val cameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)
        val camera = Camera.open(cameraId) ?: return null
        return OpenCamera(
            cameraId,
            camera,
            CameraFacing.values()[cameraInfo.facing],
            cameraInfo.orientation
        )
    }



}