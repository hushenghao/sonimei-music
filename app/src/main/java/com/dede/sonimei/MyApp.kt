package com.dede.sonimei

import android.app.Application
import android.view.ViewConfiguration
import com.dede.sonimei.net.HttpUtil
import com.squareup.leakcanary.LeakCanary


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

        HttpUtil.init(this)
    }

    /**
     * 强制菜单键
     */
    private fun makeActionOverflowMenuShown() {
        //devices with hardware menu button (e.g. Samsung Note) don't show action overflow menu
        try {
            val config = ViewConfiguration.get(this)
            val menuKeyField = ViewConfiguration::class.java.getDeclaredField("sHasPermanentMenuKey")
            if (menuKeyField != null) {
                menuKeyField.isAccessible = true
                menuKeyField.setBoolean(config, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}