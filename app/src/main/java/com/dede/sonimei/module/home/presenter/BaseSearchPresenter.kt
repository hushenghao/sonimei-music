package com.dede.sonimei.module.home.presenter

import com.dede.sonimei.MusicSource
import com.dede.sonimei.module.home.ISearchView
import org.jetbrains.anko.AnkoLogger

/**
 * Created by hsh on 2018/5/15.
 */
abstract class BaseSearchPresenter(protected val view: ISearchView,
                                   @MusicSource protected val source: Int)
    : AnkoLogger {

    protected var page = 1
    protected var pageSize = 10
    protected var search = ""

    fun pagerSize() = this.pageSize

    abstract fun search(search: String)
    abstract fun loadMore()
}