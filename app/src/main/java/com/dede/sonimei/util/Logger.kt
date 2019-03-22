//@file:Suppress("unused", "NOTHING_TO_INLINE")
//@file:JvmName("Logger")

package com.dede.sonimei.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


/**
 * Interface for the Anko logger.
 * Normally you should pass the logger tag to the [Log] methods, such as [Log.d] or [Log.e].
 * This can be inconvenient because you should store the tag somewhere or hardcode it,
 *   which is considered to be a bad practice.
 *
 * Instead of hardcoding tags, Anko provides an [Logger] interface. You can just add the interface to
 *   any of your classes, and use any of the provided extension functions, such as
 *   [Logger.debug] or [Logger.error].
 *
 * The tag is the simple class name by default, but you can change it to anything you want just
 *   by overriding the [loggerTag] property.
 */
interface Logger {

    companion object {

        internal const val LOG_SUFFIX = ".log"
        internal const val MSG_WRITE_LOG = 10
        internal const val MSG_FLUSH_LOG = 20

        internal var saveLog = false
        private var handler: Handler? = null

        internal lateinit var application: Application

        fun init(application: Application) {
            this.application = application
            application.registerComponentCallbacks(object : ComponentCallbacks2 {
                override fun onLowMemory() {
                    Logger.flush()
                }

                override fun onConfigurationChanged(newConfig: Configuration?) {
                }

                @SuppressLint("SwitchIntDef")
                override fun onTrimMemory(level: Int) {
                    when (level) {
                        ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                            Logger.flush()
                        }
                    }
                }
            })
        }

        /**
         * Log 保存开关
         */
        fun saveLog(b: Boolean) {
            saveLog = b
            if (saveLog) {
                val thread = LogHandlerThread()
                thread.start()
                handler = LogHandler(thread.looper)
            } else {
                flush()
            }
        }

        fun flush() {
            if (saveLog) {
                handler?.sendEmptyMessage(MSG_FLUSH_LOG)
            }
        }

        fun log(level: Int, tag: String?, message: String?) {
            val msg = Message.obtain()
            msg.what = MSG_WRITE_LOG
            msg.obj = LogInfo(System.currentTimeMillis(), level, tag, message)
            handler?.handleMessage(msg)
        }

        fun logDir(): File {
            return application.getExternalFilesDir("log")!!
        }

    }

    /**
     * The logger tag used in extension functions for the [logger].
     * Note that the tag length should not be more than 23 symbols.
     */
    val loggerTag: String
        get() = getTag(javaClass)
}

fun logger(clazz: Class<*>): Logger = object : Logger {
    override val loggerTag = getTag(clazz)
}

fun logger(tag: String): Logger = object : Logger {
    init {
        assert(tag.length <= 23) { "The maximum tag length is 23, got $tag" }
    }

    override val loggerTag = tag
}

inline fun <reified T : Any> logger(): Logger = logger(T::class.java)

/**
 * Send a log message with the [Log.VERBOSE] severity.
 * Note that the log message will not be written if the current log level is above [Log.VERBOSE].
 * The default log level is [Log.INFO].
 *
 * @param message the message text to log. `null` value will be represent as "null", for any other value
 *   the [Any.toString] will be invoked.
 * @param thr an exception to log (optional).
 *
 * @see [Log.v].
 */
fun Logger.verbose(message: Any?, thr: Throwable? = null) {
    log(this, message, thr, Log.VERBOSE,
            { tag, msg -> Log.v(tag, msg) },
            { tag, msg, thr -> Log.v(tag, msg, thr) })
}

/**
 * Send a log message with the [Log.DEBUG] severity.
 * Note that the log message will not be written if the current log level is above [Log.DEBUG].
 * The default log level is [Log.INFO].
 *
 * @param message the message text to log. `null` value will be represent as "null", for any other value
 *   the [Any.toString] will be invoked.
 * @param thr an exception to log (optional).
 *
 * @see [Log.d].
 */
fun Logger.debug(message: Any?, thr: Throwable? = null) {
    log(this, message, thr, Log.DEBUG,
            { tag, msg -> Log.d(tag, msg) },
            { tag, msg, thr -> Log.d(tag, msg, thr) })
}

/**
 * Send a log message with the [Log.INFO] severity.
 * Note that the log message will not be written if the current log level is above [Log.INFO]
 *   (it is the default level).
 *
 * @param message the message text to log. `null` value will be represent as "null", for any other value
 *   the [Any.toString] will be invoked.
 * @param thr an exception to log (optional).
 *
 * @see [Log.i].
 */
fun Logger.info(message: Any?, thr: Throwable? = null) {
    log(this, message, thr, Log.INFO,
            { tag, msg -> Log.i(tag, msg) },
            { tag, msg, thr -> Log.i(tag, msg, thr) })
}

/**
 * Send a log message with the [Log.WARN] severity.
 * Note that the log message will not be written if the current log level is above [Log.WARN].
 * The default log level is [Log.INFO].
 *
 * @param message the message text to log. `null` value will be represent as "null", for any other value
 *   the [Any.toString] will be invoked.
 * @param thr an exception to log (optional).
 *
 * @see [Log.w].
 */
fun Logger.warn(message: Any?, thr: Throwable? = null) {
    log(this, message, thr, Log.WARN,
            { tag, msg -> Log.w(tag, msg) },
            { tag, msg, thr -> Log.w(tag, msg, thr) })
}

/**
 * Send a log message with the [Log.ERROR] severity.
 * Note that the log message will not be written if the current log level is above [Log.ERROR].
 * The default log level is [Log.INFO].
 *
 * @param message the message text to log. `null` value will be represent as "null", for any other value
 *   the [Any.toString] will be invoked.
 * @param thr an exception to log (optional).
 *
 * @see [Log.e].
 */
fun Logger.error(message: Any?, thr: Throwable? = null) {
    log(this, message, thr, Log.ERROR,
            { tag, msg -> Log.e(tag, msg) },
            { tag, msg, thr -> Log.e(tag, msg, thr) })
}

private inline fun log(
        logger: Logger,
        message: Any?,
        thr: Throwable?,
        level: Int,
        f: (String, String) -> Unit,
        fThrowable: (String, String, Throwable) -> Unit) {
    val tag = logger.loggerTag
    if (Log.isLoggable(tag, level)) {
        if (thr != null) {
            fThrowable(tag, message?.toString() ?: "null", thr)
        } else {
            f(tag, message?.toString() ?: "null")
        }
    }

    if (!Logger.saveLog) {
        return
    }

    Logger.log(level, tag, message?.toString() ?: "null")
}

internal class LogHandler(looper: Looper) : Handler(looper) {

    private var bos: BufferedOutputStream? = null

    private fun getBos(): BufferedOutputStream {
        if (bos == null) {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            val logFile = File(Logger.logDir(), "${format.format(Date())}${Logger.LOG_SUFFIX}")
            if (logFile.exists()) {
                logFile.delete()
            }
            bos = BufferedOutputStream(FileOutputStream(logFile), 1024 * 2)
        }
        return bos!!
    }

    override fun handleMessage(msg: Message?) {
        when (msg?.what) {
            Logger.MSG_WRITE_LOG -> {
                val info = msg.obj as LogInfo
                val logLine = String.format("%s %s/ %s: %s${System.lineSeparator()}",
                        getTimeStr(info.mills), getLevelStr(info.level), info.tag, info.message)
                getBos().write(logLine.toByteArray(Charsets.UTF_8))
            }
            Logger.MSG_FLUSH_LOG -> {
                getBos().flush()
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

internal class LogHandlerThread : HandlerThread("LOG_HANDLER_THREAD") {

    override fun quit(): Boolean {
        val b = super.quit()
        Logger.flush()
        return b
    }

    override fun quitSafely(): Boolean {
        val b = super.quitSafely()
        Logger.flush()
        return b
    }

}

internal data class LogInfo(val mills: Long, val level: Int, val tag: String?, val message: Any?)

private fun getTag(clazz: Class<*>): String {
    val tag = clazz.simpleName
    return if (tag.length <= 23) {
        tag
    } else {
        tag.substring(0, 23)
    }
}