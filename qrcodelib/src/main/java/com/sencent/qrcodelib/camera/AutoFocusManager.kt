package com.sencent.qrcodelib.camera

import android.content.Context
import android.hardware.Camera
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import com.sencent.qrcodelib.Preferences
import java.lang.ref.WeakReference
import java.util.concurrent.RejectedExecutionException

/**
 *  Create by Logan at 2018/12/14 0014
 *  自动对焦回调
 */
class AutoFocusManager(context: Context, val camera: Camera) : Camera.AutoFocusCallback {

    private val FOCUS_MODES_CALLING_AF: MutableCollection<String> = ArrayList(2)
    private var stopped: Boolean = false
    private var focusing: Boolean = false
    private val useAutoFocus: Boolean
    private var outstandingTask: AutoFocusTask? = null

    init {
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO)
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO)
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val currentFocusMode = camera.parameters.focusMode
        useAutoFocus = sharedPrefs.getBoolean(Preferences.KEY_AUTO_FOCUS, true) &&
                FOCUS_MODES_CALLING_AF.contains(currentFocusMode)
        Log.i(TAG, "Current focus mode '$currentFocusMode'; use auto focus? $useAutoFocus")
        start()
    }

    override fun onAutoFocus(success: Boolean, camera: Camera?) {
        focusing = false
        autoFocusAgainLater()
    }

    @Synchronized
    private fun autoFocusAgainLater() {
        if (!stopped && outstandingTask == null) {
            val newTask = AutoFocusTask(this)
            try {
                newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                outstandingTask = newTask
            } catch (ree: RejectedExecutionException) {
                Log.w(TAG, "Could not request auto focus", ree)
            }
        }
    }

    @Synchronized
    fun start() {
        if (useAutoFocus) {
            outstandingTask = null
            if (!stopped && !focusing) {
                try {
                    camera.autoFocus(this)
                    focusing = true
                } catch (re: RuntimeException) {
                    // Have heard RuntimeException reported in Android 4.0.x+; continue?
                    Log.w(TAG, "Unexpected exception while focusing", re)
                    // Try again later to keep cycle going
                    autoFocusAgainLater()
                }
            }
        }
    }

    @Synchronized
    private fun cancelOutstandingTask() {
        if (outstandingTask != null) {
            if (outstandingTask!!.status != AsyncTask.Status.FINISHED) {
                outstandingTask!!.cancel(true)
            }
            outstandingTask = null
        }
    }

    @Synchronized
    fun stop() {
        stopped = true
        if (useAutoFocus) {
            cancelOutstandingTask()
            // Doesn't hurt to call this even if not focusing
            try {
                camera.cancelAutoFocus()
            } catch (re: RuntimeException) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                Log.w(TAG, "Unexpected exception while cancelling focusing", re)
            }
        }
    }

    private class AutoFocusTask(manager: AutoFocusManager) : AsyncTask<Any, Any, Any>() {

        private val weakReference: WeakReference<AutoFocusManager> = WeakReference(manager)

        override fun doInBackground(vararg voids: Any): Any? {
            try {
                Thread.sleep(AUTO_FOCUS_INTERVAL_MS)
            } catch (e: InterruptedException) {
                // continue
            }
            val manager = weakReference.get()
            manager?.start()
            return null
        }
    }

    companion object {
        private val TAG = AutoFocusManager::class.java.simpleName
        private const val AUTO_FOCUS_INTERVAL_MS = 1000L
    }
}