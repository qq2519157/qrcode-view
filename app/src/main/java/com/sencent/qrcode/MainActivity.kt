package com.sencent.qrcode

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.AppCompatActivity
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun scan(view: View) {
        val optionsCompat = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.act_in, R.anim.act_out)
        when (view.id) {
            R.id.btn1 -> {
                ActivityCompat.startActivity(
                    this@MainActivity,
                    Intent(this@MainActivity, TestActivity::class.java),
                    optionsCompat.toBundle()
                )
            }
            else -> {
                ActivityCompat.startActivity(
                    this@MainActivity,
                    Intent(this@MainActivity, TestFragmentActivity::class.java),
                    optionsCompat.toBundle()
                )
            }
        }

    }


}
