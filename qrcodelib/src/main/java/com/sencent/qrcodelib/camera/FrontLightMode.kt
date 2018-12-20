package com.sencent.qrcodelib.camera

import android.content.SharedPreferences
import com.sencent.qrcodelib.Preferences

/**
 *  Create by Logan at 2018/12/14 0014
 *  闪光灯模式
 */
enum class FrontLightMode {

    /** Always on.常亮  */
    ON,
    /** On only when ambient light is low. 根据光线感应调节  */
    AUTO,
    /** Always off. 关闭 */
    OFF;


    companion object {

        private fun parse(modeString: String?): FrontLightMode {
            return if (modeString == null) OFF else valueOf(modeString)
        }

        fun readPref(sharedPrefs: SharedPreferences): FrontLightMode {
            return parse(sharedPrefs.getString(Preferences.KEY_FRONT_LIGHT_MODE, OFF.toString()))
        }
    }

}