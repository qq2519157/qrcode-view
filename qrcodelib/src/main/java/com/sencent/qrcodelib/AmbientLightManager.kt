package com.sencent.qrcodelib

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.preference.PreferenceManager
import com.sencent.qrcodelib.camera.CameraManager
import com.sencent.qrcodelib.camera.FrontLightMode

/**
 *  Create by Logan at 2018/12/14 0014
 *  光线感应感应回调,用于自动感应环境光开启/关闭闪光灯
 */
class AmbientLightManager(val context: Context) : SensorEventListener {

    private var cameraManager: CameraManager? = null
    private var lightSensor: Sensor? = null

    fun start(cameraManager: CameraManager) {
        this.cameraManager = cameraManager
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (FrontLightMode.readPref(sharedPrefs) === FrontLightMode.AUTO) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    fun stop() {
        if (lightSensor != null) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.unregisterListener(this)
            cameraManager = null
            lightSensor = null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // do nothing
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val ambientLightLux = sensorEvent.values[0]
        if (cameraManager != null) {
            if (ambientLightLux <= TOO_DARK_LUX) {
                cameraManager!!.setTorch(true)
            } else if (ambientLightLux >= BRIGHT_ENOUGH_LUX) {
                cameraManager!!.setTorch(false)
            }
        }
    }

    companion object {
        private const val TOO_DARK_LUX = 45.0f
        private const val BRIGHT_ENOUGH_LUX = 250.0f
    }
}