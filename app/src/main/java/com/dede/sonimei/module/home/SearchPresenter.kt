package com.dede.sonimei.module.home

import com.dede.sonimei.MusicSource
import com.dede.sonimei.NETEASE
import com.dede.sonimei.data.BaseData
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.net.HttpUtil
import com.dede.sonimei.sourceKey
import com.dede.sonimei.util.extends.applyFragmentLifecycle
import com.dede.sonimei.util.extends.kson.fromJson
import org.jetbrains.anko.AnkoLogger

/**
 * Created by hsh on 2018/5/15.
 */
class SearchPresenter(val view: ISearchView) : AnkoLogger {

    private var page = 1
    private var pageSize = 10
    private var search = ""
    @MusicSource
    private var source = NETEASE

    fun pagerSize() = this.pageSize

    fun search(search: String, @MusicSource source: Int) {
        this.search = search
        this.page = 1
        this.source = source
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
                .params("type", sourceKey(source))
                .params("filter", "name")
                .params("page", page.toString())
                .post()
                .applyFragmentLifecycle(view.provider())
                .map { BaseData(it) }
                .filter {
                    if (!it.trueStatus()) {
                        if (isLoadMore) this.page--
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