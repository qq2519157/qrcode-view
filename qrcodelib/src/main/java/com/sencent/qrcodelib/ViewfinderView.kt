package com.sencent.qrcodelib

import android.content.Context
import android.graphics.*
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.google.zxing.ResultPoint
import com.sencent.qrcodelib.camera.CameraManager
import java.util.ArrayList

/**
 *  Create by Logan at 2018/12/14 0014
 *
 */
class ViewfinderView : View {

    private var cameraManager: CameraManager? = null
    private val paint: Paint
    private val textPaint: TextPaint
    private var resultBitmap: Bitmap? = null
    private var maskColor: Int
    //扫描区域边框颜色
    private var frameColor: Int
    //扫描线颜色
    private var laserColor: Int
    //四角颜色
    private var cornerColor: Int
    private var resultPointColor: Int
    private var scannerAlpha: Int
    private var textPadding: Float
    private var textLocation: TextLocation
    //扫描区域提示文本
    private var labelText: String? = null
    //扫描区域提示文本颜色
    private var labelTextColor: Int = 0
    private var topOffset: Int = 0
    private var labelTextSize: Float = 0.toFloat()
    private var scannerStart = 0
    private var scannerEnd = 0
    private var isShowResultPoint: Boolean = false
    private var possibleResultPoints: ArrayList<ResultPoint> = ArrayList(5)
    private var lastPossibleResultPoints: ArrayList<ResultPoint>?

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView)
        maskColor = array.getColor(
            R.styleable.ViewfinderView_maskColor,
            ContextCompat.getColor(context, R.color.viewfinder_mask)
        )
        frameColor = array.getColor(
            R.styleable.ViewfinderView_frameColor,
            ContextCompat.getColor(context, R.color.viewfinder_frame)
        )
        cornerColor = array.getColor(
            R.styleable.ViewfinderView_cornerColor,
            ContextCompat.getColor(context, R.color.viewfinder_corner)
        )
        laserColor = array.getColor(
            R.styleable.ViewfinderView_laserColor,
            ContextCompat.getColor(context, R.color.viewfinder_laser)
        )
        resultPointColor = array.getColor(
            R.styleable.ViewfinderView_resultPointColor,
            ContextCompat.getColor(context, R.color.viewfinder_result_point_color)
        )

        labelText = array.getString(R.styleable.ViewfinderView_text)
        labelTextColor = array.getColor(
            R.styleable.ViewfinderView_textColor,
            ContextCompat.getColor(context, R.color.viewfinder_text_color)
        )
        labelTextSize = array.getDimension(
            R.styleable.ViewfinderView_textSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        )
        textPadding = array.getDimension(
            R.styleable.ViewfinderView_textPadding,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
        )

        textLocation = TextLocation.getFromInt(array.getInt(R.styleable.ViewfinderView_textLocation, 0))

        array.recycle()

        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        scannerAlpha = 0
        possibleResultPoints
        lastPossibleResultPoints = null
    }

    fun setCameraManager(cameraManager: CameraManager) {
        this.cameraManager = cameraManager
    }

    fun setMaskColor(maskColor: Int) {
        this.maskColor = maskColor
    }

    fun setFrameColor(frameColor: Int) {
        this.frameColor = frameColor
    }

    fun setCornerColor(cornerColor: Int) {
        this.cornerColor = cornerColor
    }

    fun setLaserColor(laserColor: Int) {
        this.laserColor = laserColor
    }

    fun setResultPointColor(resultPointColor: Int) {
        this.resultPointColor = resultPointColor
    }

    fun setTextPadding(padding: Float) {
        this.textPadding = padding
    }

    fun setLabelText(labelText: String) {
        this.labelText = labelText
    }

    fun setLabelTextColor(color: Int) {
        this.labelTextColor = color
    }

    fun setLabelTextColorResource(@ColorRes id: Int) {
        this.labelTextColor = ContextCompat.getColor(context, id)
    }

    fun setLabelTextSize(textSize: Float) {
        this.labelTextSize = textSize
    }

    fun isShowResultPoint(): Boolean {
        return isShowResultPoint
    }

    fun setShowResultPoint(showResultPoint: Boolean) {
        isShowResultPoint = showResultPoint
    }

    fun setTextLocation(textLocation: TextLocation) {
        this.textLocation = textLocation
    }

    fun setTopOffset(offset: Int) {
        this.topOffset = offset
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cameraManager = cameraManager ?: return
        val frame = cameraManager.getFramingRect(topOffset)
        val previewFrame = cameraManager.getFramingRectInPreview()
        if (frame == null || previewFrame == null) {
            return
        }
        if (scannerStart == 0 || scannerEnd == 0) {
            scannerStart = frame.top
            scannerEnd = frame.bottom - SCANNER_LINE_HEIGHT
        }
        val width = canvas.width
        val height = canvas.height
        // Draw the exterior (i.e. outside the framing rect) darkened
        drawExterior(canvas, frame, width, height)
        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.alpha = CURRENT_POINT_OPACITY
            canvas.drawBitmap(resultBitmap, null, frame, paint)
        } else {

            // Draw a red "laser scanner" line through the middle to show decoding is active
            //            paint.setColor(laserColor);
            //            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            //            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            // Draw a two pixel solid black border inside the framing rect
            drawFrame(canvas, frame)
            // 绘制边角
            drawCorner(canvas, frame)
            // Draw a red "laser scanner" line through the middle to show decoding is active
            drawLaserScanner(canvas, frame)
            //绘制提示信息
            drawTextInfo(canvas, frame)
            val scaleX = frame.width() / previewFrame.width().toFloat()
            val scaleY = frame.height() / previewFrame.height().toFloat()
            val currentPossible = possibleResultPoints
            val currentLast = lastPossibleResultPoints
            val frameLeft = frame.left
            val frameTop = frame.top
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null
            } else {
                possibleResultPoints = ArrayList(5)
                lastPossibleResultPoints = currentPossible
                paint.alpha = CURRENT_POINT_OPACITY
                paint.color = resultPointColor
                synchronized(currentPossible) {
                    for (point in currentPossible) {
                        canvas.drawCircle(
                            (frameLeft + (point.x * scaleX).toInt()).toFloat(),
                            (frameTop + (point.y * scaleY).toInt()).toFloat(),
                            POINT_SIZE.toFloat(), paint
                        )
                    }
                }
            }
            if (currentLast != null) {
                paint.alpha = CURRENT_POINT_OPACITY / 2
                paint.color = resultPointColor
                synchronized(currentLast) {
                    val radius = POINT_SIZE / 2.0f
                    for (point in currentLast) {
                        canvas.drawCircle(
                            (frameLeft + (point.x * scaleX).toInt()).toFloat(),
                            (frameTop + (point.y * scaleY).toInt()).toFloat(),
                            radius, paint
                        )
                    }
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(
                ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE
            )
        }
    }

    /**
     * 绘制文本
     */
    private fun drawTextInfo(canvas: Canvas, frame: Rect) {
        if (!TextUtils.isEmpty(labelText)) {
            textPaint.color = labelTextColor
            textPaint.textSize = labelTextSize
            textPaint.textAlign = Paint.Align.CENTER
            val staticLayout =
                StaticLayout(labelText, textPaint, canvas.width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true)
            if (textLocation == TextLocation.BOTTOM) {
                canvas.translate((frame.left + frame.width() / 2).toFloat(), frame.bottom + textPadding)
                staticLayout.draw(canvas)
            } else {
                canvas.translate(
                    (frame.left + frame.width() / 2).toFloat(),
                    frame.top.toFloat() - textPadding - staticLayout.height.toFloat()
                )
                staticLayout.draw(canvas)
            }
        }
    }

    /**
     * 绘制边角
     */
    private fun drawCorner(canvas: Canvas, frame: Rect) {
        paint.color = cornerColor
        //左上
        canvas.drawRect(
            frame.left.toFloat(),
            frame.top.toFloat(),
            (frame.left + CORNER_RECT_WIDTH).toFloat(),
            (frame.top + CORNER_RECT_HEIGHT).toFloat(),
            paint
        )
        canvas.drawRect(
            frame.left.toFloat(),
            frame.top.toFloat(),
            (frame.left + CORNER_RECT_HEIGHT).toFloat(),
            (frame.top + CORNER_RECT_WIDTH).toFloat(),
            paint
        )
        //右上
        canvas.drawRect(
            (frame.right - CORNER_RECT_WIDTH).toFloat(),
            frame.top.toFloat(),
            frame.right.toFloat(),
            (frame.top + CORNER_RECT_HEIGHT).toFloat(),
            paint
        )
        canvas.drawRect(
            (frame.right - CORNER_RECT_HEIGHT).toFloat(),
            frame.top.toFloat(),
            frame.right.toFloat(),
            (frame.top + CORNER_RECT_WIDTH).toFloat(),
            paint
        )
        //左下
        canvas.drawRect(
            frame.left.toFloat(),
            (frame.bottom - CORNER_RECT_WIDTH).toFloat(),
            (frame.left + CORNER_RECT_HEIGHT).toFloat(),
            frame.bottom.toFloat(),
            paint
        )
        canvas.drawRect(
            frame.left.toFloat(),
            (frame.bottom - CORNER_RECT_HEIGHT).toFloat(),
            (frame.left + CORNER_RECT_WIDTH).toFloat(),
            frame.bottom.toFloat(),
            paint
        )
        //右下
        canvas.drawRect(
            (frame.right - CORNER_RECT_WIDTH).toFloat(),
            (frame.bottom - CORNER_RECT_HEIGHT).toFloat(),
            frame.right.toFloat(),
            frame.bottom.toFloat(),
            paint
        )
        canvas.drawRect(
            (frame.right - CORNER_RECT_HEIGHT).toFloat(),
            (frame.bottom - CORNER_RECT_WIDTH).toFloat(),
            frame.right.toFloat(),
            frame.bottom.toFloat(),
            paint
        )
    }

    /**
     * 绘制扫描线
     */
    private fun drawLaserScanner(canvas: Canvas, frame: Rect) {
        paint.color = laserColor
        //线性渐变
        val linearGradient = LinearGradient(
            frame.left.toFloat(), scannerStart.toFloat(),
            frame.left.toFloat(), (scannerStart + SCANNER_LINE_HEIGHT).toFloat(),
            shadeColor(laserColor),
            laserColor,
            Shader.TileMode.MIRROR
        )
        paint.shader = linearGradient
        if (scannerStart <= scannerEnd) {
            //椭圆
            val rectF = RectF(
                (frame.left + 2 * SCANNER_LINE_HEIGHT).toFloat(),
                scannerStart.toFloat(),
                (frame.right - 2 * SCANNER_LINE_HEIGHT).toFloat(),
                (scannerStart + SCANNER_LINE_HEIGHT).toFloat()
            )
            canvas.drawOval(rectF, paint)
            scannerStart += SCANNER_LINE_MOVE_DISTANCE
        } else {
            scannerStart = frame.top
        }
        paint.shader = null
    }

    /**
     * 处理颜色模糊
     */
    fun shadeColor(color: Int): Int {
        val hax = Integer.toHexString(color)
        val result = "20" + hax.substring(2)
        return Integer.valueOf(result, 16)
    }

    /**
     * 绘制扫描区边框 Draw a two pixel solid black border inside the framing rect
     */
    private fun drawFrame(canvas: Canvas, frame: Rect) {
        paint.color = frameColor
        canvas.drawRect(
            frame.left.toFloat(),
            frame.top.toFloat(),
            (frame.right + 1).toFloat(),
            (frame.top + 2).toFloat(),
            paint
        )
        canvas.drawRect(
            frame.left.toFloat(),
            (frame.top + 2).toFloat(),
            (frame.left + 2).toFloat(),
            (frame.bottom - 1).toFloat(),
            paint
        )
        canvas.drawRect(
            (frame.right - 1).toFloat(),
            frame.top.toFloat(),
            (frame.right + 1).toFloat(),
            (frame.bottom - 1).toFloat(),
            paint
        )
        canvas.drawRect(
            frame.left.toFloat(),
            (frame.bottom - 1).toFloat(),
            (frame.right + 1).toFloat(),
            (frame.bottom + 1).toFloat(),
            paint
        )
    }

    /**
     * 绘制模糊区域 Draw the exterior (i.e. outside the framing rect) darkened
     */
    private fun drawExterior(canvas: Canvas, frame: Rect, width: Int, height: Int) {
        paint.color = maskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect(
            (frame.right + 1).toFloat(),
            frame.top.toFloat(),
            width.toFloat(),
            (frame.bottom + 1).toFloat(),
            paint
        )
        canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), paint)
    }

    fun drawViewfinder() {
        val resultBitmap = this.resultBitmap
        this.resultBitmap = null
        resultBitmap?.recycle()
        invalidate()
    }

    /**
     * 添加结果点
     */
    fun addPossibleResultPoint(point: ResultPoint) {
        if (isShowResultPoint) {
            val points = possibleResultPoints
            synchronized(points) {
                points.add(point)
                val size = points.size
                if (size > MAX_RESULT_POINTS) {
                    // trim it
                    points.subList(0, size - MAX_RESULT_POINTS / 2).clear()
                }
            }
        }
    }

    /**
     * 文本位置
     */
    enum class TextLocation(val mValue: Int) {

        TOP(0), BOTTOM(1);

        companion object {
            fun getFromInt(value: Int): TextLocation {
                for (location in TextLocation.values()) {
                    if (location.mValue == value) {
                        return location
                    }
                }
                return TextLocation.TOP
            }
        }
    }


    companion object {

        private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        private const val ANIMATION_DELAY = 20L
        private const val CURRENT_POINT_OPACITY = 0xA0
        private const val MAX_RESULT_POINTS = 20
        private const val POINT_SIZE = 6
        private const val CORNER_RECT_WIDTH = 8  //扫描区边角的宽
        private val CORNER_RECT_HEIGHT = 40 //扫描区边角的高
        private val SCANNER_LINE_MOVE_DISTANCE = 6  //扫描线移动距离
        private val SCANNER_LINE_HEIGHT = 10  //扫描线宽度

    }

}