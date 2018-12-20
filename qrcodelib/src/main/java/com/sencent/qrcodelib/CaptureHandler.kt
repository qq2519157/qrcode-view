package com.sencent.qrcodelib

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.provider.Browser
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.sencent.qrcodelib.camera.CameraManager
import java.util.*

/**
 *  Create by Logan at 2018/12/14 0014
 *
 */
class CaptureHandler(
    val captureView: CaptureView,
    decodeFormats: EnumSet<BarcodeFormat>?,
    baseHints: MutableMap<DecodeHintType, Any>?,
    characterSet: String?,
    private val cameraManager: CameraManager
) : Handler() {

    private val decodeThread: DecodeThread = DecodeThread(
        captureView, decodeFormats, baseHints, characterSet,
        ViewfinderResultPointCallback(captureView.getViewfinderView())
    )
    private var state: State? = null


    init {
        decodeThread.start()
        state = State.SUCCESS

        // Start ourselves capturing previews and decoding.
        cameraManager.startPreview()
        restartPreviewAndDecode()
    }

    override fun handleMessage(message: Message) {
        val context = captureView.context
        when {
            message.what == R.id.restart_preview -> restartPreviewAndDecode()
            message.what == R.id.decode_succeeded -> {
                state = State.SUCCESS
                val bundle = message.data
                var barcode: Bitmap? = null
                var scaleFactor = 1.0f
                if (bundle != null) {
                    val compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP)
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.size, null)
                        // Mutable copy:
                        barcode = barcode!!.copy(Bitmap.Config.ARGB_8888, true)
                    }
                    scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR)
                }
                captureView.handleDecode(message.obj as Result, barcode, scaleFactor)

            }
            message.what == R.id.decode_failed -> {// We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode)

            }
            message.what == R.id.return_scan_result -> {
                captureView.dealResult((message.obj as Result).text)
                captureView.onDestroy()

            }
            message.what == R.id.launch_product_query -> {
                val url = message.obj as String

                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intents.FLAG_NEW_DOC)
                intent.data = Uri.parse(url)

                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                var browserPackageName: String? = null
                if (resolveInfo?.activityInfo != null) {
                    browserPackageName = resolveInfo.activityInfo.packageName
                    Log.d(TAG, "Using browser in package " + browserPackageName!!)
                }

                // Needed for default Android browser / Chrome only apparently
                if (browserPackageName != null) {
                    when (browserPackageName) {
                        "com.android.browser", "com.android.chrome" -> {
                            intent.setPackage(browserPackageName)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackageName)
                        }
                    }
                }

                try {
                    context.startActivity(intent)
                } catch (ignored: ActivityNotFoundException) {
                    Log.w(TAG, "Can't find anything to handle VIEW of URI $url")
                }

            }
        }
    }

    fun quitSynchronously() {
        state = State.DONE
        cameraManager.stopPreview()
        val quit = Message.obtain(decodeThread.getHandler(), R.id.quit)
        quit.sendToTarget()
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L)
        } catch (e: InterruptedException) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded)
        removeMessages(R.id.decode_failed)
    }

    private fun restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode)
            captureView.drawViewfinder()
        }
    }

    fun recycle() {
        quitSynchronously()
        cameraManager.recycle()
    }

    private enum class State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    companion object {
        private val TAG = CaptureHandler::class.java.simpleName

    }
}