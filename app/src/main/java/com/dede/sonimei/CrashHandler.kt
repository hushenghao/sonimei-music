package com.dede.sonimei

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.widget.Toast
import com.dede.sonimei.module.home.MainActivity
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap


/**
 * @author hsh
 * @date 2017/8/21 10:47.
 * @doc
 */
class CrashHandler : Thread.UncaughtExceptionHandler, AnkoLogger {

    //用来存储设备信息和异常信息
    private val deviceInfo by lazy { LinkedHashMap<String, String>() }
    private val formatter by lazy { SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA) }

    companion object {
        private var mApplicationContext: Context? = null
        private var defaultHandler: Thread.UncaughtExceptionHandler? = null
        @SuppressLint("StaticFieldLeak")
        private var instance: CrashHandler? = null

        const val LOG_SUFFIX = ".log"

        fun instance(): CrashHandler {
            if (instance == null) {
                instance = CrashHandler()
            }
            return instance!!
        }

        fun init(application: Application) {
            this.mApplicationContext = application.applicationContext
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

            Thread.setDefaultUncaughtExceptionHandler(instance())
        }
    }

    fun crashLogDir(): File {
        return mApplicationContext!!.getExternalFilesDir("log")
    }

    fun isLog(f: File?): Boolean {
        if (f == null) return false
        if (!f.isFile) return false
        if (f.name.endsWith(LOG_SUFFIX) && f.length() > 0) {
            return true
        }
        return false
    }

    override fun uncaughtException(t: Thread, e: Throwable?) {
        error("uncaughtException", e)
        if (!handleException(e)) {
            defaultHandler?.uncaughtException(t, e)
        } else {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            restart()
        }
    }

    private fun restart() {
        if (mApplicationContext != null && !BuildConfig.DEBUG) {
            val intent = Intent(mApplicationContext, MainActivity::class.java)
            val restartIntent = PendingIntent.getActivity(mApplicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val alarmManager = mApplicationContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, restartIntent)
        }
        //退出程序
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(1)
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null || mApplicationContext == null) {
            return false
        }
        ex.printStackTrace()
        //收集设备参数信息
        collectDeviceInfo(mApplicationContext!!)

        //使用Toast来显示异常信息
        doAsync {
            Looper.prepare()
            Toast.makeText(mApplicationContext,
                    R.string.app_crash,
                    Toast.LENGTH_SHORT).show()
            Looper.loop()
        }
        //保存日志文件
        saveCatchInfo2File(ex)
        return true
    }

    /**
     * 保存日志文件，不需要读写SD卡权限
     */
    private fun saveCatchInfo2File(ex: Throwable) {
        if (BuildConfig.DEBUG) return

        val sb = StringBuilder()
        for ((key, value) in deviceInfo) {
            sb.append("$key=$value\n")
        }

        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)
        var fos: FileOutputStream? = null
        try {
            val timestamp = System.currentTimeMillis()
            val time = formatter.format(Date())
            val fileName = "crash-$time-$timestamp$LOG_SUFFIX"
            fos = FileOutputStream(crashLogDir().absolutePath + File.separator + fileName)
            fos.write(sb.toString().toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
            }
        }
    }

    private fun collectDeviceInfo(context: Context) {
        try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            deviceInfo["versionName"] = pi.versionName
            deviceInfo["versionCode"] = pi.versionCode.toString()

            val fields = Build::class.java.declaredFields
            fields.forEach {
                it.isAccessible = true
                val name = it.name
                val value = it.get(null)
                deviceInfo[name] = value.toString()
                info("$name : $value")
            }

            val declaredFields = Build.VERSION::class.java.declaredFields
            declaredFields.forEach {
                it.isAccessible = true
                val name = it.name
                val value = it.get(null)
                deviceInfo[name] = value.toString()
                info("$name : $value")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}