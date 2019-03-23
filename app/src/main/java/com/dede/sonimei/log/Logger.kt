//@file:Suppress("unused", "NOTHING_TO_INLINE")
//@file:JvmName("Logger")

package com.dede.sonimei.log

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.android.parcel.Parcelize
import java.io.File


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

    /**
     * The logger tag used in extension functions for the [logger].
     * Note that the tag length should not be more than 23 symbols.
     */
    val loggerTag: String
        get() = getTag(javaClass)

    companion object {

        private var saveLog = false
        private lateinit var application: Application
        private var thread: LogThread? = null

        fun init(application: Application) {
            Companion.application = application
            application.registerComponentCallbacks(object : ComponentCallbacks2 {
                override fun onLowMemory() {
                    flush()
                }

                override fun onConfigurationChanged(newConfig: Configuration?) {
                }

                override fun onTrimMemory(level: Int) {
                    if (level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
                        flush()
                    }
                }
            })
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                private val activityList = ArrayList<Activity?>()
                override fun onActivityPaused(activity: Activity?) {
                }

                override fun onActivityResumed(activity: Activity?) {
                }

                override fun onActivityStarted(activity: Activity?) {
                }

                override fun onActivityDestroyed(activity: Activity?) {
                    activityList.remove(activity)
                    if (activityList.isEmpty()) {
                        saveLog(false)
                    }
                }

                override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
                }

                override fun onActivityStopped(activity: Activity?) {
                }

                override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                    activityList.add(activity)
                }
            })
        }

        /**
         * Log 保存开关
         */
        fun saveLog(b: Boolean) {
            saveLog = b
            if (saveLog) {
                thread = LogThread()
                thread?.start()
                Log.i("LOGGER", "start")
            } else {
                Log.i("LOGGER", "quit")
                thread?.quit()
            }
        }

        fun flush() {
            if (!saveLog) return
            thread?.flush()
        }

        fun log(level: Int, tag: String?, message: String?) {
            if (!saveLog) {
                return
            }
            val logInfo = LogInfo(System.currentTimeMillis(), level, tag, message)
            thread?.log(logInfo)
        }

        fun logDir(): File {
            return application.getExternalFilesDir("log")!!
        }

        internal fun hasPermission(): Boolean {
            return PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(application, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

@Parcelize
internal data class LogInfo(val mills: Long, val level: Int, val tag: String?, val message: String?) : Parcelable

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

    Logger.log(level, tag, message?.toString() ?: "null")
}

private fun getTag(clazz: Class<*>): String {
    val tag = clazz.simpleName
    return if (tag.length <= 23) {
        tag
    } else {
        tag.substring(0, 23)
    }
}