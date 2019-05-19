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
import com.dede.sonimei.util.Logger
import com.dede.sonimei.util.error
import com.dede.sonimei.util.info
import org.jetbrains.anko.doAsync


/**
 * @author hsh
 * @date 2017/8/21 10:47.
 * @doc
 */
class CrashHandler : Thread.UncaughtExceptionHandler, Logger {

    //用来存储设备信息和异常信息
    private val deviceInfo by lazy { LinkedHashMap<String, String>() }

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
        return true
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