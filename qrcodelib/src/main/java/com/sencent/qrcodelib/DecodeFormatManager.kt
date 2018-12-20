package com.sencent.qrcodelib

import android.content.Intent
import android.net.Uri
import com.google.zxing.BarcodeFormat
import java.util.*
import java.util.regex.Pattern

/**
 *  Create by Logan at 2018/12/14 0014
 *
 */
object DecodeFormatManager {

    private val COMMA_PATTERN = Pattern.compile(",")

    val PRODUCT_FORMATS: EnumSet<BarcodeFormat> = EnumSet.of(
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.RSS_14,
        BarcodeFormat.RSS_EXPANDED
    )
    val INDUSTRIAL_FORMATS: EnumSet<BarcodeFormat> = EnumSet.of(
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.CODE_128,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR
    )
    val ONE_D_FORMATS: EnumSet<BarcodeFormat>
    val QR_CODE_FORMATS: EnumSet<BarcodeFormat> = EnumSet.of(BarcodeFormat.QR_CODE)
    val DATA_MATRIX_FORMATS: EnumSet<BarcodeFormat> = EnumSet.of(BarcodeFormat.DATA_MATRIX)
    //去掉多余的格式提高解析效率
    val AZTEC_FORMATS: EnumSet<BarcodeFormat> = EnumSet.of(BarcodeFormat.AZTEC)
    val PDF417_FORMATS: EnumSet<BarcodeFormat> = EnumSet.of(BarcodeFormat.PDF_417)
    private val FORMATS_FOR_MODE: MutableMap<String, EnumSet<BarcodeFormat>>

    init {
        ONE_D_FORMATS = EnumSet.copyOf(PRODUCT_FORMATS)
        ONE_D_FORMATS.addAll(INDUSTRIAL_FORMATS)
    }

    init {
        FORMATS_FOR_MODE = HashMap()
        FORMATS_FOR_MODE[Intents.Scan.ONE_D_MODE] = ONE_D_FORMATS
        FORMATS_FOR_MODE[Intents.Scan.PRODUCT_MODE] = PRODUCT_FORMATS
        FORMATS_FOR_MODE[Intents.Scan.QR_CODE_MODE] = QR_CODE_FORMATS
        FORMATS_FOR_MODE[Intents.Scan.DATA_MATRIX_MODE] = DATA_MATRIX_FORMATS
        FORMATS_FOR_MODE[Intents.Scan.AZTEC_MODE] = AZTEC_FORMATS
        FORMATS_FOR_MODE[Intents.Scan.PDF417_MODE] = PDF417_FORMATS
    }

    internal fun parseDecodeFormats(intent: Intent): EnumSet<BarcodeFormat>? {
        var scanFormats: Iterable<String>? = null
        val scanFormatsString = intent.getStringExtra(Intents.Scan.FORMATS)
        if (scanFormatsString != null) {
            scanFormats = Arrays.asList(*COMMA_PATTERN.split(scanFormatsString))
        }
        return parseDecodeFormats(scanFormats, intent.getStringExtra(Intents.Scan.MODE))
    }

    internal fun parseDecodeFormats(inputUri: Uri): EnumSet<BarcodeFormat>? {
        var formats: List<String>? = inputUri.getQueryParameters(Intents.Scan.FORMATS)
        if (formats != null && formats.size == 1) {
            formats = Arrays.asList(*COMMA_PATTERN.split(formats[0]))
        }
        return parseDecodeFormats(formats, inputUri.getQueryParameter(Intents.Scan.MODE))
    }

    private fun parseDecodeFormats(scanFormats: Iterable<String>?, decodeMode: String?): EnumSet<BarcodeFormat>? {
        if (scanFormats != null) {
            val formats = EnumSet.noneOf(BarcodeFormat::class.java)
            try {
                for (format in scanFormats) {
                    formats.add(BarcodeFormat.valueOf(format))
                }
                return formats
            } catch (iae: IllegalArgumentException) {
                // ignore it then
            }

        }
        return if (decodeMode != null) {
            FORMATS_FOR_MODE[decodeMode]
        } else null
    }

}