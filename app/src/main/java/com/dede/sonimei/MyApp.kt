package com.dede.sonimei

import android.app.Application
import android.view.ViewConfiguration
import com.dede.sonimei.net.HttpUtil
import com.squareup.leakcanary.LeakCanary
import com.tencent.bugly.crashreport.CrashReport


/**
 * Created by hsh on 2018/5/14.
 */
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        makeActionOverflowMenuShown()

        if (!LeakCanary.isInAnalyzerProcess(this)) {
            LeakCanary.install(this)
        }

        CrashHandler.init(this)

        // 开发设备
        CrashReport.setIsDevelopmentDevice(applicationContext, BuildConfig.DEBUG)
        // bugly 第三个参数调试开关
//        CrashReport.initCrashReport(applicationContext, BuildConfig.BUGLY_APPID, BuildConfig.DEBUG)
        HttpUtil.init(this)
    }

    /**
     * 强制菜单键弹出PopMenu
     */
    private fun makeActionOverflowMenuShown() {
        //devices with hardware menu button (e.g. Samsung Note) don't show action overflow menu
        try {
            val config = ViewConfiguration.get(this)
            val menuKeyField = ViewConfiguration::class.java.getDeclaredField("sHasPermanentMenuKey")
            menuKeyField.isAccessible = true
            menuKeyField.setBoolean(config, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}