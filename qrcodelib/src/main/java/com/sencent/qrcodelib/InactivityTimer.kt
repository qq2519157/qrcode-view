package com.sencent.qrcodelib

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.BatteryManager
import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.RejectedExecutionException

/**
 *  Create by Logan at 2018/12/14 0014
 *
 */
class InactivityTimer(val captureView: CaptureView) {

    private var powerStatusReceiver: BroadcastReceiver = PowerStatusReceiver(this)
    private var registered: Boolean = false
    private var inactivityTask: InactivityAsyncTask? = null
    private val context = captureView.context

    init {
        registered = false
        onActivity()
    }

    @Synchronized
    fun onActivity() {
        cancel()
        inactivityTask = InactivityAsyncTask(captureView)
        try {
            inactivityTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } catch (ree: RejectedExecutionException) {
            Log.w(TAG, "Couldn't schedule inactivity task; ignoring")
        }

    }

    @Synchronized
    fun onPause() {
        cancel()
        if (registered) {
            context.unregisterReceiver(powerStatusReceiver)
            registered = false
        } else {
            Log.w(TAG, "PowerStatusReceiver was never registered?")
        }
    }

    @Synchronized
    fun onResume() {
        if (registered) {
            Log.w(TAG, "PowerStatusReceiver was already registered?")
        } else {
            context.registerReceiver(powerStatusReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            registered = true
        }
        onActivity()
    }

    @Synchronized
    private fun cancel() {
        val task = inactivityTask
        if (task != null) {
            task.cancel(true)
            inactivityTask = null
        }
    }

    fun shutdown() {
        cancel()
    }

    internal inner class PowerStatusReceiver(inactivityTimer: InactivityTimer) : BroadcastReceiver() {

        private val weakReference: WeakReference<InactivityTimer> = WeakReference(inactivityTimer)

        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                // 0 indicates that we're on battery
                val inactivityTimer = weakReference.get()
                if (inactivityTimer != null) {
                    val onBatteryNow = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) <= 0
                    if (onBatteryNow) {
                        inactivityTimer.onActivity()
                    } else {
                        inactivityTimer.cancel()
                    }
                }
            }
        }
    }

    private class InactivityAsyncTask(captureView: CaptureView) : AsyncTask<Any, Any, Any>() {

        private val weakReference: WeakReference<CaptureView> = WeakReference(captureView)

        override fun doInBackground(vararg objects: Any): Any? {
            try {
                Thread.sleep(INACTIVITY_DELAY_MS)
                Log.i(TAG, "Finishing activity due to inactivity")
                val captureView = weakReference.get()
                captureView?.onDestroy()
            } catch (e: InterruptedException) {
                // continue without killing
            }
            return null
        }
    }

    companion object {

        private val TAG = InactivityTimer::class.java.simpleName
        private const val INACTIVITY_DELAY_MS = 5 * 60 * 1000L
    }
}