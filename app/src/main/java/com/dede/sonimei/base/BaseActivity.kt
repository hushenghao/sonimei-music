package com.dede.sonimei.base

import android.os.Bundle
import android.os.StrictMode
import androidx.annotation.LayoutRes
import com.dede.sonimei.BuildConfig
import com.dede.sonimei.util.Logger
import com.dede.sonimei.util.info
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity


/**
 * Created by hsh on 2018/5/14.
 */
abstract class BaseActivity : RxAppCompatActivity(), Logger {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectCustomSlowCalls()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build())
        }
        super.onCreate(savedInstanceState)
        info("onCreate")

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val layoutId = getLayoutId()
        if (layoutId > 0) {
            setContentView(layoutId)
        }

        info("initView")
        initView(savedInstanceState)

        info("loadData")
        loadData()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    open fun loadData() {}

    open fun initView(savedInstanceState: Bundle?) {}

    @LayoutRes
    open fun getLayoutId(): Int {
        return -1
    }

    override fun onStart() {
        super.onStart()
        info("onStart")
    }

    override fun onRestart() {
        super.onRestart()
        info("onRestart")
    }

    override fun onStop() {
        super.onStop()
        info("onStop")
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        info("onSaveInstanceState")
    }

    override fun onDestroy() {
        super.onDestroy()
        info("onDestroy")
    }

}