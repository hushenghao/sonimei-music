package com.dede.sonimei.util

import android.os.Handler
import android.os.Looper
import com.dede.sonimei.log.Logger
import com.dede.sonimei.log.warn


/**
 * Created by hsh on 2018/12/29 4:40 PM
 */
class ClickEventHelper(private val callback: Callback?, private val clickDelay: Long = 300) : Logger {

    init {
        if (clickDelay <= 0) {
            throw IllegalArgumentException("clickDelay time must > 0")
        }
    }

    private var clickCount = 0

    private val clickRunnable = Runnable {
        when (clickCount) {
            1 -> {
                callback?.onClick()
                clickCount = 0
            }
            2 -> {
                callback?.onDoubleClick()
                clickCount = 0
            }
            3 -> {
                callback?.onTripleClick()
                clickCount = 0
            }
            else -> {
                warn("More Click Event, ignore. clickCount:$clickCount")
                clickCount = 0
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    fun sendClickEvent() {
        clickCount++
        handler.removeCallbacks(clickRunnable)
        handler.postDelayed(clickRunnable, clickDelay)
    }

    interface Callback {
        fun onClick()
        fun onDoubleClick()
        fun onTripleClick()
    }

}