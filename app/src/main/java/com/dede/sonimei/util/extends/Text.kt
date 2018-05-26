package com.dede.sonimei.util.extends

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
