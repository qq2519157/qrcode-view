package com.sencent.qrcode

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.annotation.FloatRange
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout

/**
 *  Create by Logan at 2018/12/17 0017
 *
 */
object StatusBarUtils {

    fun immersiveStatusBar(activity: Activity, toolbar: Toolbar) {
        immersiveStatusBar(activity, toolbar, 0.0f)
    }

    fun immersiveStatusBar(activity: Activity, toolbar: Toolbar?, @FloatRange(from = 0.0, to = 1.0) alpha: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return
        }

        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        val decorView = window.decorView as ViewGroup
        val contentView = window.decorView.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
        val rootView = contentView.getChildAt(0)
        if (rootView != null) {
            rootView.fitsSystemWindows = false
        }
        toolbar?.setPadding(0, getStatusBarHeight(activity), 0, 0)

        decorView.addView(createStatusBarView(activity, alpha))
    }

    private fun createStatusBarView(activity: Activity, @FloatRange(from = 0.0, to = 1.0) alpha: Float): View {
        // 绘制一个和状态栏一样高的矩形
        val statusBarView = View(activity)
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity))
        statusBarView.layoutParams = params
        statusBarView.setBackgroundColor(Color.argb((alpha * 255).toInt(), 0, 0, 0))
        statusBarView.id = R.id.translucent_view
        return statusBarView
    }

    /** 获取状态栏高度  */
    fun getStatusBarHeight(context: Context): Int {
        return context.resources.getDimensionPixelSize(R.dimen.status_bar_height)
    }
}