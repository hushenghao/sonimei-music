package com.dede.sonimei

import android.app.Activity
import android.app.Application
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.dede.sonimei.module.home.MainActivity
import com.dede.sonimei.net.HttpUtil
import com.liulishuo.okdownload.OkDownload
import com.liulishuo.okdownload.core.Util
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher
import com.squareup.leakcanary.LeakCanary
import com.tencent.bugly.Bugly
import com.tencent.bugly.BuglyStrategy
import com.tencent.bugly.beta.Beta
import com.tencent.bugly.beta.UpgradeInfo
import com.tencent.bugly.beta.ui.UILifecycleListener


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

        if (!BuildConfig.DEBUG) CrashHandler.init(this)

        initBugly()

        HttpUtil.init(this)

        val intent = Intent(this, StartService::class.java)
        startService(intent)
    }

    class StartService : IntentService("start") {

        override fun onHandleIntent(intent: Intent?) {
            if (BuildConfig.DEBUG) Util.enableConsoleLog()

            val okDownload = OkDownload.Builder(this)
                    .downloadDispatcher(DownloadDispatcher())
                    .build()
            try {
                OkDownload.setSingletonInstance(okDownload)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
            DownloadDispatcher.setMaxParallelRunningCount(1)
        }
    }

    private fun initBugly() {
        // 除了开发渠道以外都开启Bugly
        if ("dev" == BuildConfig.FLAVOR) return

        Beta.largeIconId = R.mipmap.ic_launcher
        Beta.smallIconId = R.mipmap.ic_launcher
        Beta.enableNotification = true
        Beta.defaultBannerId = R.mipmap.ic_launcher
        Beta.autoCheckUpgrade = true
        Beta.canShowUpgradeActs.add(MainActivity::class.java)
        Beta.enableHotfix = false
        Beta.autoDownloadOnWifi = true
        Beta.upgradeDialogLayoutId = R.layout.dialog_update_layout
        Beta.upgradeDialogLifecycleListener = object : UILifecycleListener<UpgradeInfo> {
            override fun onCreate(p0: Context?, p1: View?, p2: UpgradeInfo?) {
                val activity = p0 as? Activity ?: return
                // 透明状态栏
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    activity.window.statusBarColor = Color.TRANSPARENT
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }
            }

            override fun onResume(p0: Context?, p1: View?, p2: UpgradeInfo?) {
            }

            override fun onPause(p0: Context?, p1: View?, p2: UpgradeInfo?) {
            }

            override fun onStart(p0: Context?, p1: View?, p2: UpgradeInfo?) {
            }

            override fun onStop(p0: Context?, p1: View?, p2: UpgradeInfo?) {
            }

            override fun onDestroy(p0: Context?, p1: View?, p2: UpgradeInfo?) {
            }
        }

        Bugly.init(applicationContext,
                BuildConfig.BUGLY_APPID,
                BuildConfig.DEBUG,
                BuglyStrategy().setAppChannel(BuildConfig.CHANNEL))
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