package com.dede.sonimei.base

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.components.support.RxFragment
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Created by hsh on 2018/5/14.
 */
abstract class BaseFragment : RxFragment(), AnkoLogger {

    private var contentView: View? = null
    private var viewFirstCreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info("onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (contentView == null) {
            viewFirstCreated = true
            contentView = inflater.inflate(getLayoutId(), container, false)
        } else {
            viewFirstCreated = false
            val parent = contentView!!.parent
            if (parent != null) {
                (parent as ViewGroup).removeView(contentView)
            }
        }
        info("onCreateView")
        return contentView
    }

    @LayoutRes
    abstract fun getLayoutId(): Int

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        info("onViewCreated")
        if (viewFirstCreated) {
            info("initView")
            initView(savedInstanceState)
        }
    }

    open fun initView(savedInstanceState: Bundle?) {}

    /**
     * Fragment第一次可见的时候调用
     */
    open fun loadData() {}

    /**
     * Fragment每次可见的时候调用
     */
    open fun everyLoad() {}

    private var isLoaded = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (isVisible && userVisibleHint) {
            if (!isLoaded) {
                info("loadData")
                loadData()
                isLoaded = true
            }
            info("everyLoad")
            everyLoad()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && isVisible) {
            if (!isLoaded) {
                info("loadData")
                loadData()
                isLoaded = true
            }
            info("everyLoad")
            everyLoad()
        }
    }

    override fun onStart() {
        super.onStart()
        info("onStart")
    }

    override fun onStop() {
        super.onStop()
        info("onStop")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        info("onSaveInstanceState")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        info("onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        info("onDestroy")
    }
}