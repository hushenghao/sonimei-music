package com.dede.sonimei.module.searchresult

import com.dede.sonimei.MusicSource
import com.dede.sonimei.NETEASE
import com.dede.sonimei.SEARCH_NAME
import com.dede.sonimei.data.BaseData
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.net.HttpUtil
import com.dede.sonimei.sourceKey
import com.dede.sonimei.util.extends.applyFragmentLifecycle
import com.dede.sonimei.util.extends.kson.fromExposeJson
import com.dede.sonimei.util.extends.notNull
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
    private var type = SEARCH_NAME

    fun pagerSize() = this.pageSize

    fun search(search: String, pair: Pair<Int, String>) {
        this.search = search
        this.page = 1
        this.source = pair.first
        this.type = pair.second
        view.showLoading()
        loadList(false, this.search)
    }

    fun search(search: String) {
        this.search = search
        this.page = 1
        view.showLoading()
        loadList(false, this.search)
    }

    fun research() {
        if (search.notNull()) {
            this.search(this.search, this.source to this.type)
        } else {
            view.hideLoading()
        }
    }


    fun setTypeSource(pair: Pair<Int, String>) {
        this.source = pair.first
        this.type = pair.second
    }

    fun getTypeSource(): Pair<Int, String> {
        return source to type
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
                .params("filter", type)
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
                .map { it.fromExposeJson<ArrayList<SearchSong>>() }
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