package com.dede.sonimei.util.extends

import android.content.Context
import android.support.annotation.ColorRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.dede.sonimei.net.GlideApp
import org.jetbrains.anko.dip

/**
 * Created by hsh on 2018/5/15.
 */
fun ImageView.load(url: String?) {
    GlideApp.with(this)
            .load(url)
            .transform(RoundedCorners(this.context.dip(2)))
            .transition(withCrossFade())
            .into(this)
}

fun Context.color(@ColorRes res: Int) = ContextCompat.getColor(this, res)
fun Fragment.color(@ColorRes res: Int) = ContextCompat.getColor(this.context!!, res)

fun View?.gone() {
    this?.visibility = View.GONE
}

fun View?.show() {
    this?.visibility = View.VISIBLE
}

fun View?.hide() {
    this?.visibility = View.INVISIBLE
}