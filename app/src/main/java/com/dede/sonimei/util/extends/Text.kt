package com.dede.sonimei.util.extends

import android.content.Context
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.text.Html
import android.text.Spanned
import android.text.format.DateUtils
import java.util.*

/**
 * Created by hsh on 2018/5/15.
 */
fun String?.isNull() = this == null || this.isEmpty() || this.isBlank()

fun String?.notNull() = !this.isNull()

/**
 * 毫秒值转字符串时间
 */
fun Int.toTime(): String {
    return this.toLong().toTime()
}

/**
 * 毫秒值转时间
 */
fun Long.toTime(): String {
    val m = (this / DateUtils.MINUTE_IN_MILLIS).toInt()
    val s = (this / DateUtils.SECOND_IN_MILLIS % 60).toInt()
    val mm = String.format(Locale.getDefault(), "%02d", m)
    val ss = String.format(Locale.getDefault(), "%02d", s)
    return "$mm:$ss"
}


/**
 * Html
 */

fun String.toHtml(): Spanned = Html.fromHtml(this)

fun String.color(@ColorInt color: Int): String {
    if (this.isNull()) return this
    val colorStr = Integer.toHexString(color)
    return color("#" + colorStr.substring(colorStr.length - 6, colorStr.length))
}

fun String.color(colorStr: String): String = if (this.notNull()) "<font color=\"" +
        colorStr + "\">" + this + "</font>" else ""

fun String.color(context: Context, @ColorRes colorRes: Int): String {
    return color(ContextCompat.getColor(context, colorRes))
}

/**
 * 删除线
 */
fun String.del(): String = "<del>$this</del>"

/**
 * 加粗
 */
fun String.strong(): String = "<strong>$this</strong>"
