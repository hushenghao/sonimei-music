package com.dede.sonimei.module.search.netresult

import android.annotation.SuppressLint
import android.os.Bundle
import com.dede.sonimei.*
import com.dede.sonimei.data.BaseData
import com.dede.sonimei.data.search.NetEaseWebResult
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.net.HttpUtil
import com.dede.sonimei.util.extends.applyFragmentLifecycle
import com.dede.sonimei.util.extends.kson.fromExposeJson
import com.dede.sonimei.util.extends.kson.fromJson
import com.dede.sonimei.util.extends.notNull
import org.jetbrains.anko.AnkoLogger
import org.json.JSONObject

/**
 * Created by hsh on 2018/5/15.
 */
class SearchResultPresenter(private val view: ISearchResultView) : AnkoLogger {

    private var page = 1
    var pagerSize = 10
        private set
    private var search = ""
    @MusicSource
    private var source = normalSource
    private var type = normalType

    fun saveInstance(outState: Bundle) {
        outState.putString("type", type)
        outState.putInt("source", source)
        outState.putInt("page", page)
        outState.putInt("offset", offset)
        outState.putString("search", search)
    }

    fun loadInstance(saveInstance: Bundle?) {
        if (saveInstance == null) return
        type = saveInstance.getString("type", type)
        source = saveInstance.getInt("source", source)
        page = saveInstance.getInt("page", 1)
        offset = saveInstance.getInt("offset", 0)
        search = saveInstance.getString("search", "")
    }


    fun search(search: String, pair: Pair<Int, String>) {
        this.search = search
        this.page = 1
        this.source = pair.first
        this.type = pair.second
        offset = 0
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

    private var offset = 0 //网易云web版搜索有用到

    @SuppressLint("CheckResult")
    private fun loadList(isLoadMore: Boolean, search: String) {
        if (source == NETEASE_WEB) {
            HttpUtil.Builder()
                    .url("http://music.163.com/api/cloudsearch/pc")
                    .params("s", search)
                    .params("type", "1")
                    .params("offset", offset.toString())
                    .params("limit", pagerSize.toString())
                    .post()
                    .map { JSONObject(it) }
                    .applyFragmentLifecycle(view.provider())
                    .filter {
                        if (it.optInt("code") == 200) {
                            true
                        } else {
                            view.hideLoading()
                            view.loadError(isLoadMore)
                            false
                        }
                    }
                    .map { it.optJSONObject("result").optString("songs","[]") }
                    .map { it.fromJson<ArrayList<NetEaseWebResult>>().map { it.map() } }
                    .subscribe({
                        offset += it.size
                        view.hideLoading()
                        view.loadSuccess(isLoadMore, it)
                    }) {
                        view.loadError(isLoadMore)
                        it.printStackTrace()
                    }
            return
        }

        HttpUtil.Builder()
                .header("X-Requested-With", "XMLHttpRequest")
                .params("input", search)
                .params("type", sourceKey(source))
                .params("filter", type)
                .params("page", page.toString())
                .post()
                .map { BaseData(it) }
                .applyFragmentLifecycle(view.provider())
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