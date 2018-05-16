package com.dede.sonimei.module.home

import android.os.Bundle
import android.util.SparseArray
import com.dede.sonimei.MusicSource
import com.dede.sonimei.NETEASE
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.data.search.SearchData
import com.dede.sonimei.module.home.presenter.PresenterHelper
import com.dede.sonimei.sourceName
import com.dede.sonimei.util.extends.isNull
import com.dede.sonimei.util.extends.notNull
import com.trello.rxlifecycle2.LifecycleProvider
import kotlinx.android.synthetic.main.fragment_search_result.*
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

/**
 * Created by hsh on 2018/5/15.
 */
class SearchResultFragment : BaseFragment(), ISearchView {

    companion object {
        const val BUNDLE_SOURCE_KEY = "music_source"
        private val fragments = SparseArray<SearchResultFragment>()

        fun newInstance(@MusicSource source: Int): SearchResultFragment {
            return fragments.get(source) ?: let {
                val fragment = SearchResultFragment()
                val bundle = Bundle()
                bundle.putInt(BUNDLE_SOURCE_KEY, source)
                fragment.arguments = bundle
                fragments.put(source, fragment)
                fragment
            }
        }
    }

    private fun getSearchText(): String? {
        if (activity is MainActivity) {
            return (activity as MainActivity).searchText
        }
        return null
    }

    override fun showLoading() {
        swipe_refresh.isRefreshing = true
    }

    override fun hideLoading() {
        swipe_refresh.isRefreshing = false
    }

    override fun loadSuccess(isLoadMore: Boolean, list: List<SearchData>) {
        if (isLoadMore) {
            adapter.addData(list)
            if (list.size >= presenter.pagerSize()) {
                adapter.loadMoreComplete()
            } else {
                adapter.loadMoreEnd()
            }
        } else {
            adapter.setNewData(list)
        }
    }

    override fun loadError(isLoadMore: Boolean, msg: String?) {
        if (isLoadMore) {
            adapter.loadMoreFail()
        } else {
            hideLoading()
            toast(msg ?: "网络错误")
        }
    }

    override fun provider(): LifecycleProvider<*> = this

    @MusicSource
    private val source by lazy { arguments?.getInt(BUNDLE_SOURCE_KEY) ?: NETEASE }

    private val presenter by lazy {
        PresenterHelper.instancePresenter(source, this)
    }
    private lateinit var adapter: SearchResultAdapter

    override fun getLayoutId() = R.layout.fragment_search_result

    override fun initView(savedInstanceState: Bundle?) {
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent)
        swipe_refresh.setOnRefreshListener {
            if (searchText.notNull())
                presenter.search(searchText!!)
            else
                hideLoading()
        }
        adapter = SearchResultAdapter()
        adapter.setOnLoadMoreListener({ presenter.loadMore() }, recycler_view)
        recycler_view.adapter = adapter
    }

    override fun everyLoad() {
        val searchText = getSearchText()
        if (searchText != this.searchText && searchText.notNull())
            search(searchText)
    }

    private var searchText: String? = null

    fun search(search: String?) {
        if (search.isNull()) return

        this.searchText = search
        info(sourceName(source) + " 搜索： " + this.searchText)
        presenter.search(this.searchText!!)
    }

}