package com.sencent.qrcodelib

import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback

/**
 *  Create by Logan at 2018/12/14 0014
 *
 */
class ViewfinderResultPointCallback(private val viewfinderView: ViewfinderView) : ResultPointCallback {

    override fun foundPossibleResultPoint(point: ResultPoint) {
        viewfinderView.addPossibleResultPoint(point)
    }
}