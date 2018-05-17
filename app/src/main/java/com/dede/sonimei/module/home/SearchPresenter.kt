package com.dede.sonimei.module.home

import com.dede.sonimei.MusicSource
import com.dede.sonimei.data.BaseData
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.net.HttpUtil
import com.dede.sonimei.sourceKey
import com.dede.sonimei.util.extends.applyLifecycle
import com.dede.sonimei.util.extends.kson.fromJson
import org.jetbrains.anko.AnkoLogger

/**
 * Created by hsh on 2018/5/15.
 */
class SearchPresenter(val view: ISearchView, @MusicSource source: Int) : AnkoLogger {

    private val type = sourceKey(source)

    private var page = 1
    private var pageSize = 10
    private var search = ""

    fun pagerSize() = this.pageSize

    fun search(search: String) {
        this.search = search
        this.page = 1
        view.showLoading()
        loadList(false, this.search)
    }

    fun loadMore() {
        this.page++
        loadList(true, this.search)
    }

    private fun loadList(isLoadMore: Boolean, search: String) {
        HttpUtil.Builder()
                .header("X-Requested-With", "XMLHttpRequest")
                .params("input", search)
                .params("type", type)
                .params("filter", "name")
                .params("page", page.toString())
                .post()
                .applyLifecycle(view.provider())
                .map { BaseData(it) }
                .filter {
                    if (!it.trueStatus()) {
                        this.page--
                        view.hideLoading()
                        view.loadError(isLoadMore, it.error)
                    }
                    it.trueStatus()
                }
                .map { it.data }
                .map { it.fromJson<ArrayList<SearchSong>>() }
                .subscribe({
                    view.hideLoading()
                    view.loadSuccess(isLoadMore, it)
                }) {
                    this.page--
                    view.loadError(isLoadMore)
                    it.printStackTrace()
                }
    }
}