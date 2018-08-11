package com.dede.sonimei.module.play

import android.support.annotation.DrawableRes
import android.support.annotation.IntDef
import android.support.annotation.StringRes
import com.dede.sonimei.R

/**
 * Created by hsh on 2018/8/6.
 */
const val MODE_ORDER = 0// 顺序播放
const val MODE_SINGLE = 1// 单曲循环
const val MODE_RANDOM = 2// 随机播放

@Retention(AnnotationRetention.RUNTIME)
@IntDef(MODE_ORDER, MODE_SINGLE, MODE_RANDOM)
annotation class PlayMode

/**
 * 获取播放模式对应的图片资源id
 */
@DrawableRes
fun getPlayModeDrawRes(@PlayMode mode: Int): Int {
    return when (mode) {
        MODE_ORDER -> R.drawable.ic_mode_order
        MODE_SINGLE -> R.drawable.ic_mode_single
        MODE_RANDOM -> R.drawable.ic_mode_random
        else -> R.drawable.ic_mode_order
    }
}

@StringRes
fun getPlayModeStrRes(@PlayMode mode: Int):Int {
    return when (mode) {
        MODE_ORDER -> R.string.play_mode_order
        MODE_SINGLE -> R.string.play_mode_single
        MODE_RANDOM -> R.string.play_mode_random
        else -> R.string.play_mode_order
    }
}
