package com.sencent.qrcode

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

/**
 *  Create by Logan at 2018/12/18 0018
 *
 */
class TestPagerAdapter(val context: Context, fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                ScanFragment()
            }
            else -> {
                QRCodeFragment()
            }
        }
    }

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> {
                "扫一扫"
            }
            else -> {
                "二维码"
            }
        }
    }
}