package com.dede.sonimei.base

import android.os.Bundle
import android.support.annotation.LayoutRes
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import org.jetbrains.anko.AnkoLogger


/**
 * Created by hsh on 2018/5/14.
 */
abstract class BaseActivity : RxAppCompatActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setContentView(getLayoutId())

        initView(savedInstanceState)

        loadData()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    open fun loadData() {}

    open fun initView(savedInstanceState: Bundle?) {}

    @LayoutRes
    abstract fun getLayoutId(): Int

}