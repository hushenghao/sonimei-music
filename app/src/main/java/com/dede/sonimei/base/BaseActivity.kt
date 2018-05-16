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

        setContentView(getLayoutId())

        initView(savedInstanceState)

        loadData()
    }

    open fun loadData() {}

    open fun initView(savedInstanceState: Bundle?) {}

    @LayoutRes
    abstract fun getLayoutId(): Int

}