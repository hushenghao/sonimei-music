package com.dede.sonimei.util.extends

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
    val ss = 1000
    val mi = 60000
    val hh = 3600000


    val hour = this / 3600000
    val minute = (this - hour * hh) / mi
    val second = (this - hour * hh - minute * mi) / ss

    val sb = StringBuffer()
    if (hour > 0) {
        sb.append(hour.toString())
                .append(":")
    }

    if (minute > 0) {
        if (minute < 10) {
            sb.append("0")
        }
        sb.append(minute.toString())
    } else {
        sb.append("00")
    }
    sb.append(":")

    if (second > 0) {
        if (second < 10) {
            sb.append("0")
        }
        sb.append(second.toString())
    } else {
        sb.append("00")
    }

    return sb.toString()
}
