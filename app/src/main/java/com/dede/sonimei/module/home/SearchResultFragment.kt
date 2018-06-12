package com.dede.sonimei.module.home

import android.os.Bundle
import android.text.Html
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.download.DownloadHelper
import com.dede.sonimei.util.extends.color
import com.dede.sonimei.util.extends.isNull
import com.dede.sonimei.util.extends.load
import kotlinx.android.synthetic.main.fragment_search_result.*
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.support.v4.toast


/**
 * Created by hsh on 2018/5/15.
 */
class SearchResultFragment : BaseFragment(), ISearchView {

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

    private val presenter by lazy { SearchPresenter(this) }

    // 列表适配器
    private lateinit var adapter: BaseQuickAdapter<SearchSong, BaseViewHolder>

    override fun getLayoutId() = R.layout.fragment_search_result

    override fun initView(savedInstanceState: Bundle?) {
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent)
        swipe_refresh.setOnRefreshListener {
            presenter.research()
        }
//        val manager = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        manager.primaryClip = ClipData.newPlainText(null, adapter.data[position].url)
        adapter = object : BaseQuickAdapter<SearchSong, BaseViewHolder>(R.layout.item_search_result) {
            override fun convert(helper: BaseViewHolder?, item: SearchSong?) {
                helper?.setText(R.id.tv_name, item?.title)
                        ?.setText(R.id.tv_singer_album, item?.author)
                        ?.addOnClickListener(R.id.iv_download)
                helper?.getView<ImageView>(R.id.iv_album_img)?.load(item?.pic)
            }
        }
        adapter.setOnLoadMoreListener({ presenter.loadMore() }, rv_search_list)
        rv_search_list.adapter = adapter
        adapter.setOnItemChildClickListener { _, _, position ->
            if (position >= adapter.data.size) return@setOnItemChildClickListener
            val song = adapter.data[position] ?: return@setOnItemChildClickListener
            DownloadHelper.getInstance(context!!)
                    .download(song)
        }
        adapter.setOnItemClickListener { _, _, position ->
            if (position >= adapter.data.size) return@setOnItemClickListener
            val song = adapter.data[position] ?: return@setOnItemClickListener
            if (activity is MainActivity) {
                (activity as MainActivity).playSong(song)
            }
        }

        val tvEmpty = TextView(context)
        tvEmpty.text = Html.fromHtml(resources.getString(R.string.empty_help))
        tvEmpty.textSize = 12.5f
        val dip = dip(15)
        tvEmpty.setPadding(dip, dip, dip, 0)
        tvEmpty.setTextColor(context!!.color(R.color.text2))
        adapter.emptyView = tvEmpty
    }

    fun search(search: String?, pair: Pair<Int, String>) {
        if (search.isNull()) return
        if (!userVisibleHint || !isVisible) return

        presenter.search(search!!, pair)
    }

    fun setTypeSource(pair: Pair<Int, String>) {
        presenter.setTypeSource(pair)
    }

    fun getTypeSource(): Pair<Int, String> {
        return presenter.getTypeSource()
    }

    fun search(search: String?) {
        if (search.isNull()) return
        if (!userVisibleHint || !isVisible) return

        presenter.search(search!!)
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