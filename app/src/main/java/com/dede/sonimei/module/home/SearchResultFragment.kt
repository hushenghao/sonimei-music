package com.dede.sonimei.module.home

import android.os.Bundle
import android.util.SparseArray
import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.MusicSource
import com.dede.sonimei.NETEASE
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.download.DownloadHelper
import com.dede.sonimei.sourceName
import com.dede.sonimei.util.extends.isNull
import com.dede.sonimei.util.extends.load
import com.dede.sonimei.util.extends.notNull
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

        fun destoryInstacne(@MusicSource search: Int) {
            fragments.remove(search)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destoryInstacne(source)
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

    override fun loadSuccess(isLoadMore: Boolean, list: List<SearchSong>) {
        if (isLoadMore) {
            adapter.addData(list)
            if (list.size >= presenter.pagerSize()) {
                adapter.loadMoreComplete()
            } else {
                adapter.loadMoreEnd()
            }
        } else {
            adapter.setNewData(list)
            if (list.size < presenter.pagerSize()) {
                adapter.loadMoreEnd()
            }
        }
    }

    override fun loadError(isLoadMore: Boolean, msg: String?) {
        if (isLoadMore) {
            adapter.loadMoreFail()
        } else {
            hideLoading()
        }
        toast(msg ?: "网络错误")
    }

    override fun provider() = this

    @MusicSource
    private val source by lazy { arguments?.getInt(BUNDLE_SOURCE_KEY) ?: NETEASE }

    private val presenter by lazy { SearchPresenter(this, source) }

    // 列表适配器
    private lateinit var adapter: BaseQuickAdapter<SearchSong, BaseViewHolder>

    override fun getLayoutId() = R.layout.fragment_search_result

    override fun initView(savedInstanceState: Bundle?) {
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent)
        swipe_refresh.setOnRefreshListener {
            if (searchText.notNull())
                presenter.search(searchText!!)
            else
                hideLoading()
        }
//        val manager = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        manager.primaryClip = ClipData.newPlainText(null, adapter.data[position].url)
        adapter = object : BaseQuickAdapter<SearchSong, BaseViewHolder>(R.layout.item_search_result) {
            override fun convert(helper: BaseViewHolder?, item: SearchSong?) {
                helper?.setText(R.id.tv_name, item?.title)
                helper?.getView<ImageView>(R.id.iv_album_img)?.load(item?.pic)
                helper?.setText(R.id.tv_singer_album, item?.author)
            }
        }
        adapter.setOnLoadMoreListener({ presenter.loadMore() }, recycler_view)
        recycler_view.adapter = adapter
        adapter.setOnItemClickListener { _, _, position ->
            if (position >= adapter.data.size) return@setOnItemClickListener
            val song = adapter.data[position] ?: return@setOnItemClickListener
            DownloadHelper.getInstance(context!!)
                    .download(song)
        }
        adapter.setEmptyView(R.layout.layout_search_empty)
    }


    override fun everyLoad() {
        val searchText = getSearchText()
        if (searchText != this.searchText && searchText.notNull())
            search(searchText)
    }

    private var searchText: String? = null

    fun search(search: String?) {
        if (search.isNull()) return
        if (!userVisibleHint || !isVisible) return

        this.searchText = search
        info(sourceName(source) + " 搜索： " + this.searchText)
        presenter.search(this.searchText!!)
    }

    override fun onDestroyView() {
        /**
         * 在网络请求被取消时，SwipeRefreshLayout是加载中的状态，
         * 在Fragment重新createView的后状态没有恢复，会无法下拉。在这里恢复状态
         */
        hideLoading()
        super.onDestroyView()
    }

}