package com.sencent.qrcode

import android.app.Application
import com.squareup.leakcanary.LeakCanary

/**
 *  Create by Logan at 2018/12/18 0018
 *
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        // Normal app init code...
    }
}