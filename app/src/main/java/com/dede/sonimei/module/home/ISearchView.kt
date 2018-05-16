package com.dede.sonimei.module.home

import com.dede.sonimei.data.search.SearchData
import com.trello.rxlifecycle2.LifecycleProvider

/**
 * Created by hsh on 2018/5/15.
 */
interface ISearchView {

    fun showLoading()

    fun hideLoading()

    fun loadSuccess(isLoadMore: Boolean, list: List<SearchData>)

    fun loadError(isLoadMore: Boolean, msg: String? = null)

    fun provider(): LifecycleProvider<*>
}