package com.dede.sonimei.module.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Html
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.download.DownloadHelper
import com.dede.sonimei.util.extends.*
import com.google.gson.GsonBuilder
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
    private lateinit var adapter: ListAdapter

    override fun getLayoutId() = R.layout.fragment_search_result

    override fun initView(savedInstanceState: Bundle?) {
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent)
        swipe_refresh.setOnRefreshListener {
            presenter.research()
        }
        adapter = ListAdapter()
        adapter.setOnLoadMoreListener({ presenter.loadMore() }, rv_search_list)
        rv_search_list.adapter = adapter
        adapter.setOnItemChildClickListener { _, _, position ->
            if (position >= adapter.data.size) return@setOnItemChildClickListener
            val song = adapter.data[position] ?: return@setOnItemChildClickListener
            DownloadHelper.getInstance(context!!)
                    .download(song)
        }
        adapter.setOnItemClickListener { adapter, _, position ->
            val listAdapter = (adapter as ListAdapter)
            if (position >= listAdapter.data.size) return@setOnItemClickListener
            val song = listAdapter.data[position]
            listAdapter.onItemClick(position)
            if (activity != null && activity is MainActivity) {
                (activity as MainActivity).playSong(song)
            }
        }
        adapter.setOnItemLongClickListener { adapter, _, position ->
            val listAdapter = (adapter as ListAdapter)
            if (position >= listAdapter.data.size) return@setOnItemLongClickListener false
            val song = listAdapter.data[position]
            showDialog(song, position)
            return@setOnItemLongClickListener true
        }

        val tvEmpty = TextView(context)
        tvEmpty.text = Html.fromHtml(resources.getString(R.string.empty_help))
        tvEmpty.textSize = 12f
        tvEmpty.setLineSpacing(8f, 1f)
        tvEmpty.setPadding(dip(15), dip(10), dip(15), 0)
        tvEmpty.setTextColor(context!!.color(R.color.text2))
        adapter.emptyView = tvEmpty
    }

    private val ITEM_PLAY = "播放"
    private val ITEM_DOWNLOAD = "下载"
    private val ITEM_COPY_URL = "复制下载链接"
    private val ITEM_COPY_SOURCE = "复制音乐链接"
    private val ITEM_COPY_JSON = "复制Json数据"

    private val dialogItems by lazy {
        arrayOf(ITEM_PLAY, ITEM_DOWNLOAD, ITEM_COPY_URL, ITEM_COPY_SOURCE/*, ITEM_COPY_JSON*/)
    }

    private fun showDialog(song: SearchSong, position: Int) {
        AlertDialog.Builder(context!!)
                .setTitle("选项")
                .setItems(dialogItems) { _, which ->
                    if (which >= dialogItems.size) return@setItems
                    val s = dialogItems[which]
                    when (s) {
                        ITEM_PLAY -> {
                            adapter.onItemClick(position)
                            if (activity != null && activity is MainActivity) {
                                (activity as MainActivity).playSong(song)
                            }
                        }
                        ITEM_DOWNLOAD -> {
                            DownloadHelper.getInstance(context!!)
                                    .download(song)
                        }
                        ITEM_COPY_SOURCE -> copy(song.link)
                        ITEM_COPY_URL -> copy(song.url)
                        ITEM_COPY_JSON -> copy(GsonBuilder().create().toJson(song))
                    }
                }
                .create()
                .show()
    }

    private fun copy(string: String?) {
        if (string.isNull()) {
            toast("内容为空")
            return
        }
        val manager = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.primaryClip = ClipData.newPlainText(null, string)
        toast("已复制到剪切板")
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

    /**
     * 列表适配器
     */
    class ListAdapter : BaseQuickAdapter<SearchSong, BaseViewHolder>(R.layout.item_search_result) {

        private var clickPosition = -1

        fun onItemClick(position: Int) {
            if (position == this.clickPosition) return
            notifyItemChanged(this.clickPosition)
            this.clickPosition = position
            notifyItemChanged(position)
        }

        override fun setNewData(data: List<SearchSong>?) {
            this.clickPosition = -1
            super.setNewData(data)
        }

        override fun convert(helper: BaseViewHolder?, item: SearchSong?) {
            helper?.setText(R.id.tv_name, item?.title)
                    ?.setText(R.id.tv_singer_album, item?.author)
                    ?.addOnClickListener(R.id.iv_download)
            val ivPlaying = helper?.getView<ImageView>(R.id.iv_playing)
            val ivAlbum = helper?.getView<ImageView>(R.id.iv_album_img)
            if (helper?.layoutPosition == clickPosition) {
                ivAlbum.gone()
                ivPlaying.show()
            } else {
                ivAlbum.show()
                ivPlaying.gone()
                ivAlbum?.load(item?.pic)
            }
        }
    }

}