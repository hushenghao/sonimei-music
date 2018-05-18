package com.dede.sonimei.module.home

import com.dede.sonimei.data.search.SearchSong
import com.trello.rxlifecycle2.LifecycleProvider
import com.trello.rxlifecycle2.android.FragmentEvent

/**
 * Created by hsh on 2018/5/15.
 */
interface ISearchView {

    fun showLoading()

    fun hideLoading()

    fun loadSuccess(isLoadMore: Boolean, list: List<SearchSong>)

    fun loadError(isLoadMore: Boolean, msg: String? = null)

    fun provider(): LifecycleProvider<FragmentEvent>
}