package com.dede.sonimei.log

import android.os.Handler
import android.os.Looper
import android.os.Message

internal class LogThread : Thread() {

    private lateinit var handler: Handler

    override fun run() {
        Looper.prepare()
        handler = LogHandler(Looper.myLooper()!!)
        Looper.loop()
    }

    fun log(logInfo: LogInfo) {
        val msg = Message.obtain()
        msg.what = MSG_WRITE_LOG
        msg.obj = logInfo
        handler.handleMessage(msg)
    }

    fun flush() {
        handler.sendEmptyMessage(MSG_FLUSH_LOG)
    }

    fun quit() {
        handler.sendEmptyMessageDelayed(MSG_CLOSE_LOG,5000)
    }
}