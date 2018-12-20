package com.sencent.qrcodelib

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.sencent.qrcodelib.camera.CameraManager
import com.sencent.qrcodelib.camera.FrontLightMode
import java.io.IOException
import java.util.*

/**
 *  Create by Logan at 2018/12/17 0017
 *
 */
class CaptureView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs), SurfaceHolder.Callback {

    private lateinit var cameraManager: CameraManager
    private var handler: CaptureHandler? = null
    private var savedResultToShow: Result? = null
    private var viewfinderView: ViewfinderView
    private var surfaceView: SurfaceView
    private var lastResult: Result? = null
    private var hasSurface: Boolean = false
    private var decodeFormats: EnumSet<BarcodeFormat>? = null
    private var decodeHints: MutableMap<DecodeHintType, Any>? = null
    private var characterSet: String? = null
    private var inactivityTimer: InactivityTimer
    private var beepManager: BeepManager
    private var ambientLightManager: AmbientLightManager
    private var scanListener: ScanListener? = null
    private var maskColor: Int
    //扫描区域边框颜色
    private var frameColor: Int
    //扫描线颜色
    private var laserColor: Int
    //四角颜色
    private var cornerColor: Int
    //结果点的颜色
    private var resultPointColor: Int
    //扫描区域提示文本内间距
    private var textPadding: Float
    //扫描区域向向下
    private var topOffset: Int
    //文本位置,上面或者下面
    private var textLocation: ViewfinderView.TextLocation
    //扫描区域提示文本
    private var labelText: String? = null
    //扫描区域提示文本颜色
    private var labelTextColor: Int = 0
    private var labelTextSize: Float = 0.toFloat()
    //是否支持缩放（变焦），默认支持
    private var isZoom = true
    private var oldDistance: Float = 0.toFloat()
    private var isShowResultPoint: Boolean = false

    init {
        val rootView = LayoutInflater.from(context).inflate(R.layout.layout_capture_view, this, true)
        val array = context.obtainStyledAttributes(attrs, R.styleable.CaptureView)
        maskColor = array.getColor(
            R.styleable.CaptureView_cv_maskColor,
            ContextCompat.getColor(context, R.color.viewfinder_mask)
        )
        frameColor = array.getColor(
            R.styleable.CaptureView_cv_frameColor,
            ContextCompat.getColor(context, R.color.viewfinder_frame)
        )
        cornerColor = array.getColor(
            R.styleable.CaptureView_cv_cornerColor,
            ContextCompat.getColor(context, R.color.viewfinder_corner)
        )
        laserColor = array.getColor(
            R.styleable.CaptureView_cv_laserColor,
            ContextCompat.getColor(context, R.color.viewfinder_laser)
        )
        resultPointColor = array.getColor(
            R.styleable.CaptureView_cv_resultPointColor,
            ContextCompat.getColor(context, R.color.viewfinder_result_point_color)
        )

        labelText = array.getString(R.styleable.CaptureView_cv_text)
        labelTextColor = array.getColor(
            R.styleable.CaptureView_cv_textColor,
            ContextCompat.getColor(context, R.color.viewfinder_text_color)
        )
        labelTextSize = array.getDimension(
            R.styleable.CaptureView_cv_textSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        )
        textPadding = array.getDimension(
            R.styleable.CaptureView_cv_textPadding,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
        )
        topOffset = array.getInt(
            R.styleable.CaptureView_cv_topOffset,
            0
        )
        textLocation = ViewfinderView.TextLocation.getFromInt(array.getInt(R.styleable.CaptureView_cv_textLocation, 0))
        array.recycle()
        hasSurface = false
        inactivityTimer = InactivityTimer(this)
        beepManager = BeepManager(this)
        ambientLightManager = AmbientLightManager(context)
        viewfinderView = rootView.findViewById(R.id.viewfinder_view)
        surfaceView = rootView.findViewById(R.id.preview_view)
        viewfinderView.setMaskColor(maskColor)
        viewfinderView.setFrameColor(frameColor)
        viewfinderView.setCornerColor(cornerColor)
        viewfinderView.setLaserColor(laserColor)
        viewfinderView.setResultPointColor(resultPointColor)
        viewfinderView.setLabelText(labelText ?: "")
        viewfinderView.setLabelTextColor(labelTextColor)
        viewfinderView.setLabelTextSize(labelTextSize)
        viewfinderView.setTextPadding(textPadding)
        viewfinderView.setShowResultPoint(isShowResultPoint)
        viewfinderView.setTextLocation(textLocation)
        viewfinderView.setTopOffset(topOffset)
    }

    /**
     * 设置扫描状态监听
     */
    fun setScanListener(scanListener: ScanListener) {
        this.scanListener = scanListener
    }

    /**
     * 设置边框阴影区域颜色
     */
    fun setMaskColor(maskColor: Int) {
        this.maskColor = maskColor
        viewfinderView.setMaskColor(maskColor)
    }

    fun setMaskColorResource(@ColorRes id: Int) {
        this.maskColor = ContextCompat.getColor(context, id)
        viewfinderView.setMaskColor(ContextCompat.getColor(context, id))
    }

    /**
     * 设置扫描区域边框颜色
     */
    fun setFrameColor(frameColor: Int) {
        this.frameColor = frameColor
        viewfinderView.setFrameColor(frameColor)
    }

    fun setFrameColorResource(@ColorRes id: Int) {
        this.frameColor = ContextCompat.getColor(context, id)
        viewfinderView.setFrameColor(ContextCompat.getColor(context, id))
    }

    /**
     * 设置边框四角颜色
     */
    fun setCornerColor(cornerColor: Int) {
        this.cornerColor = cornerColor
        viewfinderView.setCornerColor(frameColor)
    }

    fun setCornerColorResource(@ColorRes id: Int) {
        this.cornerColor = ContextCompat.getColor(context, id)
        viewfinderView.setCornerColor(ContextCompat.getColor(context, id))
    }

    /**
     * 设置结果点的颜色
     */
    fun setResultPointColor(resultPointColor: Int) {
        this.resultPointColor = resultPointColor
        viewfinderView.setResultPointColor(resultPointColor)
    }

    fun setResultPointColorResource(@ColorRes id: Int) {
        this.resultPointColor = ContextCompat.getColor(context, id)
        viewfinderView.setResultPointColor(ContextCompat.getColor(context, id))
    }


    /**
     * 设置扫描区域提示文本
     */
    fun setLabelText(labelText: String) {
        this.labelText = labelText
        viewfinderView.setLabelText(labelText)
    }

    /**
     * 设置扫描区域提示文本颜色
     */
    fun setLabelTextColor(color: Int) {
        this.labelTextColor = color
        viewfinderView.setLabelTextColor(color)
    }

    fun setLabelTextColorResource(@ColorRes id: Int) {
        this.labelTextColor = ContextCompat.getColor(context, id)
        viewfinderView.setLabelTextColor(ContextCompat.getColor(context, id))
    }

    /**
     * 扫描区域提示文本内间距
     */
    fun setTextPadding(padding: Float) {
        this.textPadding = padding
        viewfinderView.setTextPadding(padding)
    }

    /**
     * 扫描区域提示文本字体大小
     */
    fun setLabelTextSize(textSize: Float) {
        this.labelTextSize = textSize
        viewfinderView.setLabelTextSize(textSize)
    }

    /**
     * 获取当前结果点显示状态
     */
    fun isShowResultPoint(): Boolean {
        return isShowResultPoint
    }

    /**
     * 设置是否显示结果点
     */
    fun setShowResultPoint(showResultPoint: Boolean) {
        isShowResultPoint = showResultPoint
        viewfinderView.setShowResultPoint(showResultPoint)
    }

    /**
     * 设置提示文本位置
     * @param textLocation
     * [ViewfinderView.TextLocation.TOP] 顶部
     * [ViewfinderView.TextLocation.BOTTOM] 底部
     */
    fun setTextLocation(textLocation: ViewfinderView.TextLocation) {
        this.textLocation = textLocation
        viewfinderView.setTextLocation(textLocation)
    }

    fun setTopOffset(offset: Int) {
        this.topOffset = offset
        viewfinderView.setTopOffset(topOffset)
    }

    /**
     *  设置闪光灯模式
     *  @param mode
     *  [FrontLightMode.ON]常亮
     *  [FrontLightMode.OFF] 一直关闭
     *  [FrontLightMode.AUTO]光线感应自动开启或关闭
     */
    fun setFlashMode(mode: FrontLightMode) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit().apply {
            putString(Preferences.KEY_FRONT_LIGHT_MODE, mode.toString())
            apply()
        }
    }

    /**
     * 打开闪光灯
     */
    fun flashOn() {
        val camera = cameraManager.openCamera?.camera
        val parameters = camera?.parameters
        parameters?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
        camera?.parameters = parameters
    }

    /**
     * 关闭闪光灯
     */
    fun flashOff() {
        val camera = cameraManager.openCamera?.camera
        val parameters = camera?.parameters
        parameters?.flashMode = Camera.Parameters.FLASH_MODE_OFF
        camera?.parameters = parameters
    }

    /**
     *  是否有提示音(具体发声依赖当前情景模式，如：静音模式...)
     */
    fun setPlayBeep(play: Boolean) {
        beepManager.setPlayBeep(play)
    }

    /**
     *  是否有震动
     */
    fun setVibrate(vibrate: Boolean) {
        beepManager.setVibrate(vibrate)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //没有相机权限,无法正常加载
                Toast.makeText(context, "扫一扫功能需要相机权限,请授予权限后再试", Toast.LENGTH_SHORT).show()
                return
            }
        }
        onResume()
    }


    fun isCameraCanUse(): Boolean {
        var canUse = true
        try {
            val camera = Camera.open(0)
            val parameters = camera.parameters
            if (parameters != null) {
                camera?.release()
            }
        } catch (e: Exception) {
            canUse = false
        }

        return canUse
    }

    /**
     * 提示音出错
     */
    fun onBeepManagerError() {
        scanListener?.onError(BEEP_MANAGER_ERROR)
    }

    /**
     * 是否是ZXing的Url
     */
    private fun isZXingURL(dataString: String?): Boolean {
        if (dataString == null) {
            return false
        }
        for (url in ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true
            }
        }
        return false
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        // do nothing
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        hasSurface = false
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!")
        }
        if (!hasSurface) {
            hasSurface = true
            initCamera(holder)
        }
    }

    /**
     * 重新开始扫描
     */
    fun restartPreviewAfterDelay(delayMS: Long) {
        if (handler != null) {
            handler!!.sendEmptyMessageDelayed(R.id.restart_preview, delayMS)
        }
        resetStatusView()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isZoom && cameraManager.isOpen()) {
            val camera = cameraManager.openCamera?.camera ?: return super.onTouchEvent(event)
            if (event.pointerCount == 1) {//单点触控，聚焦
                //                focusOnTouch(event,camera);
            } else {
                when (event.action and MotionEvent.ACTION_MASK) {
                    //多点触控
                    MotionEvent.ACTION_POINTER_DOWN -> oldDistance = calcFingerSpacing(event)
                    MotionEvent.ACTION_MOVE -> {
                        val newDistance = calcFingerSpacing(event)

                        if (newDistance > oldDistance + DEVIATION) {//
                            handleZoom(true, camera)
                        } else if (newDistance < oldDistance - DEVIATION) {
                            handleZoom(false, camera)
                        }
                        oldDistance = newDistance
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 获取当前缩放变焦状态
     */
    fun isZoom(): Boolean {
        return isZoom
    }

    /**
     * 设置是否缩放变焦
     */
    fun setZoom(zoom: Boolean) {
        isZoom = zoom
    }

    /**
     * 处理变焦缩放
     * @param isZoomIn
     * @param camera
     */
    private fun handleZoom(isZoomIn: Boolean, camera: Camera) {
        val params = camera.parameters
        if (params.isZoomSupported) {
            val maxZoom = params.maxZoom
            var zoom = params.zoom
            if (isZoomIn && zoom < maxZoom) {
                zoom++
            } else if (zoom > 0) {
                zoom--
            }
            params.zoom = zoom
            camera.parameters = params
        } else {
            Log.i(TAG, "zoom not supported")
        }
    }

    /**
     * 聚焦
     * @param event
     * @param camera
     */
    fun focusOnTouch(event: MotionEvent, camera: Camera) {
        val params = camera.parameters
        val previewSize = params.previewSize
        val focusRect = calcTapArea(event.rawX, event.rawY, 1f, previewSize)
        val meteringRect = calcTapArea(event.rawX, event.rawY, 1.5f, previewSize)
        val parameters = camera.parameters
        if (parameters.maxNumFocusAreas > 0) {
            val focusAreas = ArrayList<Camera.Area>()
            focusAreas.add(Camera.Area(focusRect, 600))
            parameters.focusAreas = focusAreas
        }
        if (parameters.maxNumMeteringAreas > 0) {
            val meteringAreas = ArrayList<Camera.Area>()
            meteringAreas.add(Camera.Area(meteringRect, 600))
            parameters.meteringAreas = meteringAreas
        }
        val currentFocusMode = params.focusMode
        params.focusMode = Camera.Parameters.FOCUS_MODE_MACRO
        camera.parameters = params
        camera.autoFocus { success, camera ->
            val params = camera.parameters
            params.focusMode = currentFocusMode
            camera.parameters = params
        }
    }

    /**
     * 计算两指间距离
     * @param event
     * @return
     */
    private fun calcFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    /**
     * 计算对焦区域
     * @param x
     * @param y
     * @param coefficient
     * @param previewSize
     * @return
     */
    private fun calcTapArea(x: Float, y: Float, coefficient: Float, previewSize: Camera.Size): Rect {
        val focusAreaSize = 200f
        val areaSize = java.lang.Float.valueOf(focusAreaSize * coefficient).toInt()
        val centerX = (x / previewSize.width * 2000 - 1000).toInt()
        val centerY = (y / previewSize.height * 2000 - 1000).toInt()
        val left = clamp(centerX - areaSize / 2, -1000, 1000)
        val top = clamp(centerY - areaSize / 2, -1000, 1000)
        val rectF = RectF(left.toFloat(), top.toFloat(), (left + areaSize).toFloat(), (top + areaSize).toFloat())
        return Rect(
            Math.round(rectF.left), Math.round(rectF.top),
            Math.round(rectF.right), Math.round(rectF.bottom)
        )
    }

    /**
     *
     * @param x
     * @param min
     * @param max
     * @return
     */
    private fun clamp(x: Int, min: Int, max: Int): Int {
        if (x > max) {
            return max
        }
        return if (x < min) {
            min
        } else x
    }

    /**
     * 初始化相机
     */
    private fun initCamera(surfaceHolder: SurfaceHolder?) {
        if (surfaceHolder == null) {
            throw IllegalStateException("No SurfaceHolder provided")
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?")
            return
        }
        try {
            cameraManager.openDriver(surfaceHolder)
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = CaptureHandler(this, decodeFormats, decodeHints, characterSet, cameraManager)
            }
            decodeOrStoreSavedBitmap(null, null)
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
            //            displayFrameworkBugMessageAndExit();
        } catch (e: RuntimeException) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e)
            //            displayFrameworkBugMessageAndExit();
            initCameraError()
        }

    }

    private fun initCameraError() {
        scanListener?.onError(CAMERA_ERROR)
    }

    /**
     * 初始化未处理的bitmap(暂时未使用)
     */
    private fun decodeOrStoreSavedBitmap(bitmap: Bitmap?, result: Result?) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result
        } else {
            if (result != null) {
                savedResultToShow = result
            }
            if (savedResultToShow != null) {
                val message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow)
                handler!!.sendMessage(message)
            }
            savedResultToShow = null
        }
    }

    private fun resetStatusView() {
        viewfinderView.visibility = View.VISIBLE
        lastResult = null
    }

    fun drawViewfinder() {
        viewfinderView.drawViewfinder()
    }


    /**
     * 扫码后声音与震动管理里入口
     * @return true表示播放和震动，是否播放与震动还得看[BeepManager]
     */
    fun isBeepSoundAndVibrate(): Boolean {
        return true
    }

    fun getViewfinderView(): ViewfinderView {
        return viewfinderView
    }


    fun getCaptureHandler(): Handler? {
        return handler
    }

    fun getCameraManager(): CameraManager {
        return cameraManager
    }

    fun getBeepManager(): BeepManager {
        return beepManager
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    fun handleDecode(rawResult: Result, barcode: Bitmap?, scaleFactor: Float) {
        inactivityTimer.onActivity()
        lastResult = rawResult
        if (isBeepSoundAndVibrate()) {
            beepManager.playBeepSoundAndVibrate()
        }
        val resultString = rawResult.text
        dealResult(resultString)
    }

    /**
     * 处理结果,一般是通过实现ScanListener处理其
     *  onSuccess方法来处理最终的结果
     */
    fun dealResult(result: String) {
        scanListener?.onSuccess(result)
    }

    /**
     * 提供给调用者的恢复方法
     */
    fun onResume() {
        cameraManager = CameraManager(context)
        viewfinderView = findViewById(R.id.viewfinder_view)
        viewfinderView.setCameraManager(cameraManager)
        handler = null
        lastResult = null
        resetStatusView()
        beepManager.updatePrefs()
        ambientLightManager.start(cameraManager)
        inactivityTimer.onResume()
        val intent = when (context) {
            is AppCompatActivity -> (context as AppCompatActivity).intent
            else -> null
        }
        decodeFormats = null
        characterSet = null
        if (intent != null) {
            val action = intent.action
            val dataString = intent.dataString
            if (Intents.Scan.ACTION.equals(action)) {
                // Scan the formats the intent requested, and return the result to the calling activity.
                // source = IntentSource.NATIVE_APP_INTENT;
                decodeFormats = DecodeFormatManager.parseDecodeFormats(intent)
                decodeHints = DecodeHintManager.parseDecodeHints(intent)
                if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
                    val width = intent.getIntExtra(Intents.Scan.WIDTH, 0)
                    val height = intent.getIntExtra(Intents.Scan.HEIGHT, 0)
                    if (width > 0 && height > 0) {
                        cameraManager.setManualFramingRect(width, height)
                    }
                }
                if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
                    val cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1)
                    if (cameraId >= 0) {
                        cameraManager.setManualCameraId(cameraId)
                    }
                }
            } else if (dataString != null &&
                dataString.contains("http://www.google") &&
                dataString.contains("/m/products/scan")
            ) {
                decodeFormats = DecodeFormatManager.PRODUCT_FORMATS
            } else if (isZXingURL(dataString)) {

                val inputUri = Uri.parse(dataString)
                //scanFromWebPageManager = new ScanFromWebPageManager(inputUri);
                decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri)
                // Allow a sub-set of the hints to be specified by the caller.
                decodeHints = DecodeHintManager.parseDecodeHints(inputUri)
            }
            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET)
        }
        surfaceView = findViewById(R.id.preview_view)
        val surfaceHolder = surfaceView.holder
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder)
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this)
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
    }


    /**
     * 提供给调用者的暂停方法,当前界面不可见,但是还未销毁
     */
    fun onPause() {
        if (handler != null) {
            handler!!.recycle()
            handler = null
        }
        if (cameraManager.isOpen()) {
            cameraManager.closeDriver()
        }
        inactivityTimer.onPause()
    }


    /**
     * 销毁,结束扫描,一般用于结束activity调用
     * 需要调用者手动处理
     */
    fun onDestroy() {
        beepManager.close()
        if (handler != null) {
            handler!!.recycle()
            handler = null
        }
        inactivityTimer.onPause()
        inactivityTimer.shutdown()
        ambientLightManager.stop()
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            val surfaceView = findViewById<SurfaceView>(R.id.preview_view)
            val surfaceHolder = surfaceView.holder
            surfaceHolder.removeCallback(this)
        }
        scanListener?.onFinish()
    }


    fun setVolumeControlStream(volumControlStream: Int) {
        when (context) {
            is AppCompatActivity -> {
                val activity = context as AppCompatActivity
                activity.volumeControlStream = volumControlStream
            }
            else -> {
                //do nothing
            }
        }
    }


    companion object {

        private val TAG = CaptureView::class.java.simpleName
        val BEEP_MANAGER_ERROR = 0
        val CAMERA_ERROR = 1
        private val DEVIATION = 6
        private val ZXING_URLS = arrayOf("http://zxing.appspot.com/scan", "zxing://scan/")
    }

}