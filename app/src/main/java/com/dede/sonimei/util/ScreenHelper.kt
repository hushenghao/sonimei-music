package com.dede.sonimei.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect


/**
 * Created by hsh on 2018/6/6.
 */
object ScreenHelper {

    private val frame = Rect()

    /**
     * 获取应用显示区域的顶部到屏幕顶部的距离，可以用来简单的适配齐刘海机型
     */
    fun getFrameTopMargin(activity: Activity?): Int {
        if (frame.top > 0) {
            return frame.top
        }

        if (activity == null) {
            return frame.top
        }

        activity.window.decorView.getWindowVisibleDisplayFrame(frame)

        if (frame.top > 0) {
            return frame.top
        }

        frame.top = getStatusBarHeight(activity)

        return frame.top
    }

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources
                .getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }
}