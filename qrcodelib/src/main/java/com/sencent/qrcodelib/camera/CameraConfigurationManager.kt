package com.sencent.qrcodelib.camera

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.hardware.Camera
import android.preference.PreferenceManager
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.sencent.qrcodelib.Preferences
import com.sencent.qrcodelib.camera.open.CameraFacing
import com.sencent.qrcodelib.camera.open.OpenCamera

/**
 *  Create by Logan at 2018/12/14 0014
 *  相机初始化参数管理
 */
 class CameraConfigurationManager(private val context: Context?) {

    var cwNeededRotation: Int = 0
    private var cwRotationFromDisplayToCamera: Int = 0
    var screenResolution: Point? = null
    var cameraResolution: Point? = null
    var bestPreviewSize: Point? = null
    var previewSizeOnScreen: Point? = null

    /**
     * Reads, one time, values from the camera that are needed by the app.
     * 读取当前的相机参数
     */
    fun initFromCameraParameters(camera: OpenCamera) {
        val parameters = camera.camera.parameters
        val manager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val displayRotation = display.rotation
        val cwRotationFromNaturalToDisplay: Int
        cwRotationFromNaturalToDisplay = when (displayRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else ->
                // Have seen this return incorrect values like -90
                if (displayRotation % 90 == 0) {
                    (360 + displayRotation) % 360
                } else {
                    throw IllegalArgumentException("Bad rotation: $displayRotation")
                }
        }
        Log.i(TAG, "Display at: $cwRotationFromNaturalToDisplay")
        var cwRotationFromNaturalToCamera = camera.orientation
        Log.i(TAG, "Camera at: $cwRotationFromNaturalToCamera")
        // Still not 100% sure about this. But acts like we need to flip this:
        if (camera.facing === CameraFacing.FRONT) {
            cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360
            Log.i(TAG, "Front camera overriden to: $cwRotationFromNaturalToCamera")
        }
        cwRotationFromDisplayToCamera = (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360
        Log.i(TAG, "Final display orientation: $cwRotationFromDisplayToCamera")
        cwNeededRotation = if (camera.facing === CameraFacing.FRONT) {
            Log.i(TAG, "Compensating rotation for front camera")
            (360 - cwRotationFromDisplayToCamera) % 360
        } else {
            cwRotationFromDisplayToCamera
        }
        Log.i(TAG, "Clockwise rotation from display to camera: $cwNeededRotation")
        val theScreenResolution = Point()
        display.getSize(theScreenResolution)
        screenResolution = theScreenResolution
        Log.i(TAG, "Screen resolution in current orientation: " + screenResolution!!)

        cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution!!)
        Log.i(TAG, "Camera resolution: " + cameraResolution!!)
        bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution!!)
        Log.i(TAG, "Best available preview size: " + bestPreviewSize!!)
        val isScreenPortrait = screenResolution!!.x < screenResolution!!.y
        val isPreviewSizePortrait = bestPreviewSize!!.x < bestPreviewSize!!.y
        previewSizeOnScreen = if (isScreenPortrait == isPreviewSizePortrait) {
            bestPreviewSize
        } else {
            Point(bestPreviewSize!!.y, bestPreviewSize!!.x)
        }
        Log.i(TAG, "Preview size on screen: " + previewSizeOnScreen!!)
    }

    fun setDesiredCameraParameters(camera: OpenCamera, safeMode: Boolean) {
        val theCamera = camera.camera
        val parameters = theCamera.parameters
        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.")
            return
        }
        Log.i(TAG, "Initial camera parameters: " + parameters.flatten())

        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored")
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (parameters.isZoomSupported) {
            parameters.zoom = parameters.maxZoom / 10
        }
        theCamera.setDisplayOrientation(90)
        theCamera.parameters = parameters
        initializeTorch(parameters, prefs, safeMode)
        CameraConfigurationUtils.setFocus(
            parameters,
            prefs.getBoolean(Preferences.KEY_AUTO_FOCUS, true),
            prefs.getBoolean(Preferences.KEY_DISABLE_CONTINUOUS_FOCUS, true),
            safeMode
        )
        if (!safeMode) {
            if (prefs.getBoolean(Preferences.KEY_INVERT_SCAN, false)) {
                CameraConfigurationUtils.setInvertColor(parameters)
            }
            if (!prefs.getBoolean(Preferences.KEY_DISABLE_BARCODE_SCENE_MODE, true)) {
                CameraConfigurationUtils.setBarcodeSceneMode(parameters)
            }
            if (!prefs.getBoolean(Preferences.KEY_DISABLE_METERING, true)) {
                CameraConfigurationUtils.setVideoStabilization(parameters)
                CameraConfigurationUtils.setFocusArea(parameters)
                CameraConfigurationUtils.setMetering(parameters)
            }
            //SetRecordingHint to true also a workaround for low framerate on Nexus 4
            //https://stackoverflow.com/questions/14131900/extreme-camera-lag-on-nexus-4
            parameters.setRecordingHint(true)
        }
        parameters.setPreviewSize(bestPreviewSize!!.x, bestPreviewSize!!.y)
        theCamera.parameters = parameters
        theCamera.setDisplayOrientation(cwRotationFromDisplayToCamera)
        val afterParameters = theCamera.parameters
        val afterSize = afterParameters.previewSize
        if (afterSize != null && (bestPreviewSize!!.x != afterSize.width || bestPreviewSize!!.y != afterSize.height)) {
            Log.w(
                TAG,
                "Camera said it supported preview size " + bestPreviewSize!!.x + 'x'.toString() + bestPreviewSize!!.y +
                        ", but after setting it, preview size is " + afterSize.width + 'x'.toString() + afterSize.height
            )
            bestPreviewSize!!.x = afterSize.width
            bestPreviewSize!!.y = afterSize.height
        }
    }

    fun getTorchState(camera: Camera?): Boolean {
        if (camera != null) {
            val parameters = camera.parameters
            if (parameters != null) {
                val flashMode = parameters.flashMode
                return Camera.Parameters.FLASH_MODE_ON == flashMode || Camera.Parameters.FLASH_MODE_TORCH == flashMode
            }
        }
        return false
    }

    fun setTorch(camera: Camera, newSetting: Boolean) {
        val parameters = camera.parameters
        doSetTorch(parameters, newSetting, false)
        camera.parameters = parameters
    }

    private fun initializeTorch(parameters: Camera.Parameters, prefs: SharedPreferences, safeMode: Boolean) {
        val currentSetting = FrontLightMode.readPref(prefs) === FrontLightMode.ON
        doSetTorch(parameters, currentSetting, safeMode)
    }

    private fun doSetTorch(parameters: Camera.Parameters, newSetting: Boolean, safeMode: Boolean) {
        CameraConfigurationUtils.setTorch(parameters, newSetting)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!safeMode && !prefs.getBoolean(Preferences.KEY_DISABLE_EXPOSURE, true)) {
            CameraConfigurationUtils.setBestExposure(parameters, newSetting)
        }
    }

    companion object {
        private val TAG = CameraConfigurationManager::class.java.simpleName
    }

}