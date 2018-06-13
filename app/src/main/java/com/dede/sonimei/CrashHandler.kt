package com.dede.sonimei

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.widget.Toast
import org.jetbrains.anko.AnkoLogger
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
    private val formatter by lazy { SimpleDateFormat("yyyy-MM-dd-HH-mm-ss") }

    companion object {
        private var mApplicationContext: Context? = null
        private var defaultHandler: Thread.UncaughtExceptionHandler? = null
        @SuppressLint("StaticFieldLeak")
        private var instance: CrashHandler? = null

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

    override fun uncaughtException(t: Thread, e: Throwable?) {
        error("uncaughtException", e)
        if (!handleException(e)) {
            defaultHandler?.uncaughtException(t, e)
        } else {
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            //退出程序
//        ExitUtil.finish()
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null || mApplicationContext == null) {
            return false
        }
        ex.printStackTrace()
        //收集设备参数信息
        collectDeviceInfo(mApplicationContext!!)

        //使用Toast来显示异常信息
        object : Thread() {
            override fun run() {
                Looper.prepare()
                Toast.makeText(mApplicationContext,
                        "喵，很抱歉，程序出现异常，即将退出！",
                        Toast.LENGTH_SHORT).show()
                Looper.loop()
            }
        }.start()
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
            val fileName = "crash-$time-$timestamp.txt"
            val dir = mApplicationContext!!.getExternalFilesDir("log")
            fos = FileOutputStream(dir.absolutePath + File.separator + fileName)
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