package com.dede.sonimei.module.home.presenter

import com.dede.sonimei.NETEASE
import com.dede.sonimei.data.search.NeteasData
import com.dede.sonimei.module.home.ISearchView
import com.dede.sonimei.net.HttpUtil
import com.dede.sonimei.util.extends.applyLifecycle
import com.dede.sonimei.util.extends.kson.fromJson

/**
 * Created by hsh on 2018/5/15.
 */
class NeteasePresenter(view: ISearchView) : BaseSearchPresenter(view, NETEASE) {

    override fun search(search: String) {
        this.search = search
        this.page = 1
        view.showLoading()
        load()
    }

    override fun loadMore() {
        this.page++
        load()
    }

    private fun load() {
        val isLoadMore = page != 0
        HttpUtil.Builder()
                .params("s", search)
                .params("type", "1")
                .params("offset", (page * pageSize - pageSize).toString())
                .params("limit", pageSize.toString())
                .url("/api/cloudsearch/pc")
                .post()
                .applyLifecycle(view.provider())
                .map { it.fromJson<NeteasData>() }
                .subscribe({
                    view.hideLoading()
                    if (it.code != 200) {
                        view.loadError(isLoadMore)
                        return@subscribe
                    }
                    val songs = it.result?.songs ?: emptyList()
                    val list = songs.map { it.toSearchData() }
                    view.loadSuccess(isLoadMore, list)
                }) {
                    view.loadError(isLoadMore)
                    it.printStackTrace()
                }
    }
}