package com.sencent.qrcodelib

/**
 *  Create by Logan at 2018/12/17 0017
 *
 */
interface ScanListener {

    fun onSuccess(result: String)

    fun onError(errorCode: Int)

    fun onFinish()
}