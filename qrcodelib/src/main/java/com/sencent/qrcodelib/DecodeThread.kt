package com.sencent.qrcodelib

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPointCallback
import java.util.*

import java.util.concurrent.CountDownLatch

/**
 *  Create by Logan at 2018/12/14 0014
 *
 */
class DecodeThread(
    val captureView: CaptureView,
    var decodeFormats: EnumSet<BarcodeFormat>?,
    baseHints: MutableMap<DecodeHintType, Any>?,
    characterSet: String?,
    resultPointCallback: ResultPointCallback
) : Thread() {

    private var handlerInitLatch: CountDownLatch = CountDownLatch(1)
    private var hints: MutableMap<DecodeHintType, Any?> = EnumMap(DecodeHintType::class.java)
    private lateinit var handler: Handler

    init {
        if (baseHints != null) {
            hints.putAll(baseHints)
        }
        val context = captureView.context
        // The prefs can't change while the thread is running, so pick them up once here.
        if (decodeFormats == null || decodeFormats!!.isEmpty()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            decodeFormats = EnumSet.noneOf(BarcodeFormat::class.java)
            if (prefs.getBoolean(Preferences.KEY_DECODE_1D_PRODUCT, true)) {
                decodeFormats!!.addAll(DecodeFormatManager.PRODUCT_FORMATS)
            }
            if (prefs.getBoolean(Preferences.KEY_DECODE_1D_INDUSTRIAL, true)) {
                decodeFormats!!.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS)
            }
            if (prefs.getBoolean(Preferences.KEY_DECODE_QR, true)) {
                decodeFormats!!.addAll(DecodeFormatManager.QR_CODE_FORMATS)
            }
            if (prefs.getBoolean(Preferences.KEY_DECODE_DATA_MATRIX, true)) {
                decodeFormats!!.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS)
            }
             if (prefs.getBoolean(Preferences.KEY_DECODE_AZTEC, false)) {
                 decodeFormats!!.addAll(DecodeFormatManager.AZTEC_FORMATS)
             }
             if (prefs.getBoolean(Preferences.KEY_DECODE_PDF417, false)) {
                 decodeFormats!!.addAll(DecodeFormatManager.PDF417_FORMATS)
             }
        }
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats

        if (characterSet != null) {
            hints[DecodeHintType.CHARACTER_SET] = characterSet
        }
        hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = resultPointCallback
        Log.i("DecodeThread", "Hints: $hints")
    }

    fun getHandler(): Handler {
        try {
            handlerInitLatch.await()
        } catch (ie: InterruptedException) {
            // continue?
        }
        return handler
    }

    override fun run() {
        Looper.prepare()
        handler = DecodeHandler(captureView, hints)
        handlerInitLatch.countDown()
        Looper.loop()
    }



    companion object {
        const val BARCODE_BITMAP = "barcode_bitmap"
        const val BARCODE_SCALED_FACTOR = "barcode_scaled_factor"
    }
}