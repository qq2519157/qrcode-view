package com.sencent.qrcode

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.sencent.qrcodelib.CaptureView
import com.sencent.qrcodelib.ScanListener

/**
 *  Create by Logan at 2018/12/18 0018
 *
 */
class ScanFragment : Fragment(), ScanListener {
    private var mCaptureView: CaptureView? = null
    private var mRootView: View? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRootView = inflater.inflate(R.layout.fragmnet_scan, container, false)
        return mRootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mCaptureView = mRootView?.findViewById(R.id.captureview)
        mCaptureView?.setPlayBeep(true)
        mCaptureView?.setVibrate(true)
        mCaptureView?.setScanListener(this)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            mCaptureView?.onResume()
            Log.i(TAG, "onResume")
        } else {
            mCaptureView?.onPause()
            Log.i(TAG, "onPause")
        }
    }


    override fun onDestroy() {
        mCaptureView?.onDestroy()
        super.onDestroy()
    }


    override fun onFinish() {
        activity?.finish()
    }

    override fun onSuccess(result: String) {
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
        mCaptureView?.restartPreviewAfterDelay(300L)
    }

    override fun onError(errorCode: Int) {
        when (errorCode) {
            CaptureView.CAMERA_ERROR -> Toast.makeText(context,"相机加载异常，无法正常使用",Toast.LENGTH_SHORT).show()
            CaptureView.BEEP_MANAGER_ERROR -> {

            }
            else -> activity?.finish()
        }
        activity?.finish()
    }

    companion object {
        private val TAG = ScanFragment::class.java.simpleName
    }
}