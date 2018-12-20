package com.sencent.qrcode

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.sencent.qrcodelib.utils.ActionUtils
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 *  Create by Logan at 2018/12/18 0018
 *
 */
class TestFragmentActivity : AppCompatActivity() {
    private lateinit var mTabs: TabLayout
    private lateinit var mViewpager: ViewPager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_fragment)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_PERMISSION_CAMERA
                )
            }
        }
        mTabs = findViewById(R.id.tabs)
        mViewpager = findViewById(R.id.viewpager)
        mViewpager.offscreenPageLimit=0
        mViewpager.adapter = TestPagerAdapter(this@TestFragmentActivity, supportFragmentManager)
        mTabs.setupWithViewPager(mViewpager)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // 未获得Camera权限
                AlertDialog.Builder(this@TestFragmentActivity)
                    .setTitle("提示")
                    .setMessage("请在系统设置中为App开启摄像头权限后重试")
                    .setPositiveButton("确定") { _, _ -> finish() }
                    .show()
            }
        } else if (grantResults.isNotEmpty() && requestCode == REQUEST_PERMISSION_PHOTO) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this@TestFragmentActivity)
                    .setTitle("提示")
                    .setMessage("请在系统设置中为App中开启文件权限后重试")
                    .setPositiveButton("确定", null)
                    .show()
            } else {
                ActionUtils.startActivityForGallery(this@TestFragmentActivity, ActionUtils.PHOTO_REQUEST_GALLERY)
            }
        }
    }



    companion object {
        private val REQUEST_PERMISSION_CAMERA = 1000
        private val REQUEST_PERMISSION_PHOTO = 1001
        private val TAG=TestFragmentActivity::class.java.simpleName
    }
}