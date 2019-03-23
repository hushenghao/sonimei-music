package com.dede.sonimei.log

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_SUFFIX = ".log"
internal const val MSG_WRITE_LOG = 10
internal const val MSG_FLUSH_LOG = 20
internal const val MSG_CLOSE_LOG = 30

internal class LogHandler(looper: Looper) : Handler(looper) {

    private var out: BufferedOutputStream? = null
    private val fileName:String

    init {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        fileName = "${format.format(Date())}$LOG_SUFFIX"
    }

    private fun getBos(): BufferedOutputStream? {
        if (out == null) {
            if (!Logger.hasPermission()) return null

            val logFile = File(Logger.logDir(), fileName)
            if (logFile.exists()) {
                logFile.delete()
            }
            out = BufferedOutputStream(FileOutputStream(logFile), 1024)
        }
        return out
    }

    override fun handleMessage(msg: Message?) {
        when (msg?.what) {
            MSG_WRITE_LOG -> {
                val info = msg.obj as LogInfo
                val logLine = String.format("%s %s/ %s: %s${System.lineSeparator()}",
                        getTimeStr(info.mills), getLevelStr(info.level), info.tag, info.message)
                try {
                    getBos()?.write(logLine.toByteArray(Charsets.UTF_8))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            MSG_FLUSH_LOG -> {
                try {
                    getBos()?.flush()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            MSG_CLOSE_LOG -> {
                try {
                    getBos()?.flush()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    getBos()?.close()
                }

                looper.quitSafely()// 退出looper
            }
        }
    }

    private fun getTimeStr(mills: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)
        return format.format(mills)
    }

    private fun getLevelStr(level: Int): String {
        return when (level) {
            Log.ASSERT -> "A"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.VERBOSE -> "V"
            else -> "_"
        }
    }
}