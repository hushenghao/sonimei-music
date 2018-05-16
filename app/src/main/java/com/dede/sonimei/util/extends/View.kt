package com.dede.sonimei.util.extends

import android.content.Context
import android.support.annotation.ColorRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import com.bumptech.glide.Glide

/**
 * Created by hsh on 2018/5/15.
 */
fun ImageView.load(url: String?) {
    Glide.with(this).load(url).into(this)
}

fun Context.color(@ColorRes res: Int) = ContextCompat.getColor(this, res)
fun Fragment.color(@ColorRes res: Int) = ContextCompat.getColor(this.context!!, res)