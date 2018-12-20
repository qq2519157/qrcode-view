package com.sencent.qrcodelib.utils

import android.graphics.*
import android.support.annotation.ColorInt
import android.text.TextPaint
import android.text.TextUtils
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.sencent.qrcodelib.DecodeFormatManager
import java.util.*

/**
 *  Create by Logan at 2018/12/17 0017
 *
 */
object QRCodeUtils {

    /**
     * 生成二维码
     * @param content
     * @param heightPix
     * @return
     */
    fun createQRCode(content: String, heightPix: Int): Bitmap? {
        return createQRCode(content, heightPix, null)
    }

    /**
     * 生成二维码
     * @param content
     * @param heightPix
     * @param logo
     * @return
     */
    fun createQRCode(content: String, heightPix: Int, logo: Bitmap?): Bitmap? {
        //配置参数
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        //容错级别
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        //设置空白边距的宽度
        hints[EncodeHintType.MARGIN] = 1 //default is 4
        return createQRCode(content, heightPix, logo, hints)
    }

    /**
     * 生成二维码
     * @param content
     * @param heightPix
     * @param logo
     * @param hints
     * @return
     */
    fun createQRCode(content: String, heightPix: Int, logo: Bitmap?, hints: Map<EncodeHintType, *>): Bitmap? {
        try {

            // 图像数据转换，使用了矩阵转换
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, heightPix, heightPix, hints)
            val pixels = IntArray(heightPix * heightPix)
            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果
            for (y in 0 until heightPix) {
                for (x in 0 until heightPix) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * heightPix + x] = -0x1000000
                    } else {
                        pixels[y * heightPix + x] = -0x1
                    }
                }
            }

            // 生成二维码图片的格式
            var bitmap: Bitmap? = Bitmap.createBitmap(heightPix, heightPix, Bitmap.Config.ARGB_8888)
            bitmap!!.setPixels(pixels, 0, heightPix, 0, 0, heightPix, heightPix)

            if (logo != null) {
                bitmap = addLogo(bitmap, logo)
            }

            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 在二维码中间添加Logo图案
     */
    private fun addLogo(src: Bitmap?, logo: Bitmap?): Bitmap? {
        if (src == null) {
            return null
        }
        if (logo == null) {
            return src
        }
        //获取图片的宽高
        val srcWidth = src.width
        val srcHeight = src.height
        val logoWidth = logo.width
        val logoHeight = logo.height
        if (srcWidth == 0 || srcHeight == 0) {
            return null
        }
        if (logoWidth == 0 || logoHeight == 0) {
            return src
        }
        //logo大小为二维码整体大小的1/6
        val scaleFactor = srcWidth * 1.0f / 6f / logoWidth.toFloat()
        var bitmap: Bitmap? = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap!!)
            canvas.drawBitmap(src, 0f, 0f, null)
            canvas.scale(scaleFactor, scaleFactor, (srcWidth / 2).toFloat(), (srcHeight / 2).toFloat())
            canvas.drawBitmap(
                logo,
                ((srcWidth - logoWidth) / 2).toFloat(),
                ((srcHeight - logoHeight) / 2).toFloat(),
                null
            )
            canvas.save(Canvas.ALL_SAVE_FLAG)
            canvas.restore()
        } catch (e: Exception) {
            bitmap = null
            e.printStackTrace()
        }

        return bitmap
    }

    /**
     * 获取带圆角+边框的bitmap
     * 一般用于二维码中间添加头像
     */
    fun getRoundBitmapByShader(bitmap: Bitmap?, offset: Int, radius: Float, boarder: Float): Bitmap? {
        if (bitmap == null) {
            return null
        }
        val width = bitmap.width
        val height = bitmap.height
        val outWidth = width + offset
        val outHeight = height + offset
        val widthScale = outWidth * 1f / width
        val heightScale = outHeight * 1f / height
        val matrix = Matrix()
        matrix.setScale(widthScale, heightScale)
        //创建输出的bitmap
        val desBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        //创建canvas并传入desBitmap，这样绘制的内容都会在desBitmap上
        val canvas = Canvas(desBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        //创建着色器
        val bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = bitmapShader
        //创建矩形区域并且预留出border
        val rect = RectF(boarder, boarder, outWidth - boarder, outHeight - boarder)
        //把传入的bitmap绘制到圆角矩形区域内
        canvas.drawRoundRect(rect, radius, radius, paint);
        if (boarder > 0) {
            //绘制boarder
            val boarderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            boarderPaint.color = Color.WHITE
            boarderPaint.style = Paint.Style.STROKE
            boarderPaint.strokeWidth = boarder
            canvas.drawRoundRect(rect, radius, radius, boarderPaint)
        }
        return desBitmap
    }

    /**
     * 往当前bitmap中间添加一个小的bitmap
     * @param src 最终显示的bitmap
     * @param logo  贴的图
     * @param ratio  小图占大图比例
     */
    fun addLogo(src: Bitmap, logo: Bitmap, ratio: Float): Bitmap? {
        //获取图片的宽高
        val srcWidth = src.width
        val srcHeight = src.height
        val logoWidth = logo.width
        val logoHeight = logo.height

        if (srcWidth == 0 || srcHeight == 0) {
            return null
        }

        if (logoWidth == 0 || logoHeight == 0) {
            return src
        }
        //logo大小为二维码整体大小的ratio比例
        val scaleFactor = srcWidth * ratio / logoWidth.toFloat()
        var bitmap: Bitmap? = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap!!)
            canvas.drawBitmap(src, 0f, 0f, null)
            canvas.scale(scaleFactor, scaleFactor, (srcWidth / 2).toFloat(), (srcHeight / 2).toFloat())
            canvas.drawBitmap(
                logo,
                ((srcWidth - logoWidth) / 2).toFloat(),
                ((srcHeight - logoHeight) / 2).toFloat(),
                null
            )
            canvas.save(Canvas.ALL_SAVE_FLAG)
            canvas.restore()
        } catch (e: Exception) {
            bitmap = null
            e.printStackTrace()
        }
        return bitmap
    }

    /**
     * 解析二维码图片
     * @param bitmapPath
     * @return
     */
    fun parseQRCode(bitmapPath: String): String? {
        val hints = HashMap<DecodeHintType, Any>()
        hints[DecodeHintType.CHARACTER_SET] = "utf-8"
        return parseQRCode(bitmapPath, hints)
    }

    /**
     * 解析二维码图片
     * @param bitmapPath
     * @param hints
     * @return
     */
    fun parseQRCode(bitmapPath: String, hints: Map<DecodeHintType, *>): String? {
        try {
            val result = QRCodeReader().decode(getBinaryBitmap(compressBitmap(bitmapPath)), hints)
            return result.text
        } catch (e: Exception) {
            e.printStackTrace()

        }

        return null
    }

    /**
     * 解析一维码/二维码图片
     * @param bitmapPath
     * @return
     */
    fun parseCode(bitmapPath: String): String? {
        val hints = HashMap<DecodeHintType, Any>()
        //添加可以解析的编码类型
        val decodeFormats = Vector<BarcodeFormat>()
        decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS)
//        decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS)
//        decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS)

        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
        return parseCode(bitmapPath, hints)
    }

    /**
     * 解析一维码/二维码图片
     * @param bitmapPath
     * @param hints 解析编码类型
     * @return
     */
    fun parseCode(bitmapPath: String, hints: Map<DecodeHintType, Any>): String? {
        try {
            val reader = MultiFormatReader()
            reader.setHints(hints)
            val result = reader.decodeWithState(getBinaryBitmap(compressBitmap(bitmapPath)))
            return result.text
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 压缩图片
     * @param path
     * @return
     */
    private fun compressBitmap(path: String): Bitmap {

        val newOpts = BitmapFactory.Options()
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true//获取原始图片大小
        BitmapFactory.decodeFile(path, newOpts)// 此时返回bm为空
        val w = newOpts.outWidth
        val h = newOpts.outHeight
        val width = 800f
        val height = 480f
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        var be = 1// be=1表示不缩放
        if (w > h && w > width) {// 如果宽度大的话根据宽度固定大小缩放
            be = (newOpts.outWidth / width).toInt()
        } else if (w < h && h > height) {// 如果高度高的话根据宽度固定大小缩放
            be = (newOpts.outHeight / height).toInt()
        }
        if (be <= 0)
            be = 1
        newOpts.inSampleSize = be// 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        newOpts.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, newOpts)
    }

    /**
     * 获取二进制图片
     * @param bitmap
     * @return
     */
    private fun getBinaryBitmap(bitmap: Bitmap): BinaryBitmap {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(width, height, pixels)
        //得到二进制图片
        return BinaryBitmap(HybridBinarizer(source))
    }

    /**
     * 生成条形码
     * @param content
     * @param format
     * @param desiredWidth
     * @param desiredHeight
     * @return
     */
    fun createBarCode(content: String, format: BarcodeFormat, desiredWidth: Int, desiredHeight: Int): Bitmap? {
        return createBarCode(content, format, desiredWidth, desiredHeight, null)

    }

    /**
     * 生成条形码
     * @param content
     * @param format
     * @param desiredWidth
     * @param desiredHeight
     * @param hints
     * @return
     */
    fun createBarCode(
        content: String,
        format: BarcodeFormat,
        desiredWidth: Int,
        desiredHeight: Int,
        hints: Map<EncodeHintType, *>?
    ): Bitmap? {
        return createBarCode(content, format, desiredWidth, desiredHeight, hints, false, 40, Color.BLACK)
    }

    /**
     * 生成条形码
     * @param content
     * @param format
     * @param desiredWidth
     * @param desiredHeight
     * @param hints
     * @param isShowText
     * @return
     */
    fun createBarCode(
        content: String,
        format: BarcodeFormat,
        desiredWidth: Int,
        desiredHeight: Int,
        hints: Map<EncodeHintType, *>,
        isShowText: Boolean
    ): Bitmap? {
        return createBarCode(content, format, desiredWidth, desiredHeight, hints, isShowText, 40, Color.BLACK)
    }

    /**
     * 生成条形码
     * @param content
     * @param format
     * @param desiredWidth
     * @param desiredHeight
     * @param hints
     * @param isShowText
     * @param textSize
     * @param textColor
     * @return
     */
    fun createBarCode(
        content: String,
        format: BarcodeFormat,
        desiredWidth: Int,
        desiredHeight: Int,
        hints: Map<EncodeHintType, *>?,
        isShowText: Boolean,
        textSize: Int, @ColorInt textColor: Int
    ): Bitmap? {
        if (TextUtils.isEmpty(content)) {
            return null
        }
        val WHITE = -0x1
        val BLACK = -0x1000000

        val writer = MultiFormatWriter()
        try {
            val result = writer.encode(
                content, format, desiredWidth,
                desiredHeight, hints
            )
            val width = result.width
            val height = result.height
            val pixels = IntArray(width * height)
            // All are 0, or black, by default
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (result.get(x, y)) BLACK else WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return if (isShowText) {
                addCode(bitmap, content, textSize, textColor, textSize / 2)
            } else bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 条形码下面添加文本信息
     * @param src
     * @param code
     * @param textSize
     * @param textColor
     * @return
     */
    private fun addCode(src: Bitmap?, code: String, textSize: Int, @ColorInt textColor: Int, offset: Int): Bitmap? {
        if (src == null) {
            return null
        }

        if (TextUtils.isEmpty(code)) {
            return src
        }

        //获取图片的宽高
        val srcWidth = src.width
        val srcHeight = src.height

        if (srcWidth <= 0 || srcHeight <= 0) {
            return null
        }

        var bitmap: Bitmap? = Bitmap.createBitmap(srcWidth, srcHeight + textSize + offset * 2, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap!!)
            canvas.drawBitmap(src, 0f, 0f, null)
            val paint = TextPaint()
            paint.textSize = textSize.toFloat()
            paint.color = textColor
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(code, (srcWidth / 2).toFloat(), (srcHeight + textSize / 2 + offset).toFloat(), paint)
            canvas.save(Canvas.ALL_SAVE_FLAG)
            canvas.restore()
        } catch (e: Exception) {
            bitmap = null
            e.printStackTrace()
        }

        return bitmap
    }
}