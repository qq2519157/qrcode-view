package com.sencent.qrcodelib

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream


/**
 *  Create by Logan at 2018/12/14 0014
 *
 */
class DecodeHandler(val captureView: CaptureView, hints: Map<DecodeHintType, Any?>) : Handler() {

    private val multiFormatReader = MultiFormatReader()
    private var running = true

    init {
        multiFormatReader.setHints(hints)
    }

    override fun handleMessage(message: Message?) {
        if (message == null || !running) {
            return
        }
        if (message.what == R.id.decode) {
            decode(message.obj as ByteArray, message.arg1, message.arg2)

        } else if (message.what == R.id.quit) {
            running = false
            Looper.myLooper()!!.quit()

        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private fun decode(data: ByteArray, width: Int, height: Int) {
        var width = width
        var height = height
        val start = System.currentTimeMillis()
        var rawResult: Result? = null
        val rotatedData = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width)
                rotatedData[x * height + height - y - 1] = data[x + y * width]
        }
        val tmp = width
        width = height
        height = tmp
        val source = captureView.getCameraManager().buildLuminanceSource(rotatedData, width, height)
        if (source != null) {
            /**
             * zxing项目官方默认使用的是HybridBinarizer二值化方法。然而目前的大部分二维码都是黑色二维码，白色背景的。不管是二维码扫描还是二维码图像识别，
             * 使用GlobalHistogramBinarizer算法的效果要稍微比HybridBinarizer好一些，识别的速度更快，对低分辨的图像识别精度更高。
             */
            val bitmap = BinaryBitmap(HybridBinarizer(source))
//            val bitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap)
            } catch (re: ReaderException) {
                // continue
            } finally {
                multiFormatReader.reset()
            }
        }
        val handler = captureView.getCaptureHandler()
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            val end = System.currentTimeMillis()
            Log.d(TAG, "Found barcode in " + (end - start) + " ms")
            if (handler != null) {
                val message = Message.obtain(handler, R.id.decode_succeeded, rawResult)
                val bundle = Bundle()
                bundleThumbnail(source!!, bundle)
                message.data = bundle
                message.sendToTarget()
            }
        } else {
            if (handler != null) {
                val message = Message.obtain(handler, R.id.decode_failed)
                message.sendToTarget()
            }
        }
    }

    private fun bundleThumbnail(source: PlanarYUVLuminanceSource, bundle: Bundle) {
        val pixels = source.renderThumbnail()
        val width = source.thumbnailWidth
        val height = source.thumbnailHeight
        val bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray())
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, width.toFloat() / source.width)
    }

    companion object {
        private val TAG = DecodeHandler::class.java.simpleName
    }
}