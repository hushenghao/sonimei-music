package com.dede.sonimei.module.search.netresult

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.Html
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.download.DownloadHelper
import com.dede.sonimei.module.home.MainActivity
import com.dede.sonimei.module.setting.Settings
import com.dede.sonimei.normalSource
import com.dede.sonimei.util.extends.*
import kotlinx.android.synthetic.main.fragment_search_result.*
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.toast


/**
 * Created by hsh on 2018/5/15.
 */
class SearchResultFragment : BaseFragment(), ISearchResultView {

    override fun showLoading() {
        swipe_refresh.isRefreshing = true
    }

    override fun hideLoading() {
        swipe_refresh.isRefreshing = false
    }

    override fun loadSuccess(isLoadMore: Boolean, list: List<SearchSong>) {
        if (isLoadMore) {
            adapter.addData(list)
            if (list.size >= presenter.pagerSize) {
                adapter.loadMoreComplete()
            } else {
                adapter.loadMoreEnd()
            }
        } else {
            adapter.setNewData(list)
            if (list.size < presenter.pagerSize) {
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
        toast(msg ?: getString(R.string.net_error))
    }

    override fun provider() = this

    private val presenter by lazy { SearchResultPresenter(this) }

    // 列表适配器
    private lateinit var adapter: ListAdapter

    override fun getLayoutId() = R.layout.fragment_search_result

    override fun initView(savedInstanceState: Bundle?) {
        presenter.loadInstance(savedInstanceState)

        swipe_refresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent)
        swipe_refresh.setOnRefreshListener {
            presenter.research()
        }
        adapter = ListAdapter()
        if (savedInstanceState != null) {
            val list = savedInstanceState.getParcelableArrayList<SearchSong>("search_list")
            if (list != null && list.isNotEmpty()) {
                adapter.addData(list)
            }
        } else {
            val source = defaultSharedPreferences.getInt(Settings.KEY_DEFAULT_SEARCH_SOURCE, normalSource)
            presenter.setTypeSource(source to getTypeSource().second)
        }
        adapter.setOnLoadMoreListener({ presenter.loadMore() }, rv_search_list)
        rv_search_list.adapter = adapter
        rv_search_list.layoutManager = object : LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean {
                return if (adapter.data.size > 0) super.canScrollVertically() else false
            }
        }
        adapter.setOnItemChildClickListener { _, _, position ->
            if (position >= adapter.data.size) return@setOnItemChildClickListener
            val song = adapter.data[position] ?: return@setOnItemChildClickListener
            DownloadHelper.download(activity, song)
        }
        adapter.setOnItemClickListener { adapter, _, position ->
            val listAdapter = (adapter as ListAdapter)
            if (position >= listAdapter.data.size) return@setOnItemClickListener
            val song = listAdapter.data[position]
            if (activity != null && activity is MainActivity) {
                (activity as MainActivity).playSongs(listAdapter.data, song)
            }
        }
        adapter.setOnItemLongClickListener { adapter, _, position ->
            val listAdapter = (adapter as ListAdapter)
            if (position >= listAdapter.data.size) return@setOnItemLongClickListener false
            val song = listAdapter.data[position]
            showDialog(song, position)
            return@setOnItemLongClickListener true
        }

        adapter.setEmptyView(R.layout.layout_list_empty)
        val tvEmpty = adapter.emptyView.find<TextView>(R.id.tv_empty)
        tvEmpty.text = Html.fromHtml(resources.getString(R.string.empty_help))
    }

    private fun showDialog(song: SearchSong, position: Int) {
        AlertDialog.Builder(context!!)
                .setTitle(R.string.dialog_option)
                .setItems(R.array.dialog_items) { _, which ->
                    when (which) {
                        0 -> {
                            if (activity != null && activity is MainActivity) {
                                (activity as MainActivity).playSongs(adapter.data, song)
                            }
                        }
                        1 -> {
                            if (activity != null && activity is MainActivity) {
                                (activity as MainActivity).add2PlayList(song)
                            }
                        }
                        2 -> {
                            DownloadHelper.download(activity, song)
                        }
                        3 -> {
                            song.loadPlayLink()
                                    .applySchedulers()
                                    .subscribe({
                                        copy(song.path)
                                    }) {
                                        toast(R.string.download_link_empty)
                                        it.printStackTrace()
                                    }
                        }
                        4 -> copy(song.link)
                    }
                }
                .create()
                .show()
    }

    private fun copy(string: String?) {
        if (string.isNull()) {
            toast(R.string.copy_empty)
            return
        }
        val manager = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.primaryClip = ClipData.newPlainText(null, string)
        toast(R.string.copy_succ)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter.saveInstance(outState)
        outState.putParcelableArrayList("search_list", ArrayList(adapter.data))
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
            var ignore = false
            if (clickPosition != -1 && data != null && clickPosition < data.size) {
                if (this.data[clickPosition].link == data[clickPosition].link) {
                    ignore = true
                }
            }
            if (!ignore) this.clickPosition = -1
            super.setNewData(data)
        }

        override fun convert(helper: BaseViewHolder, item: SearchSong) {
            helper.addOnClickListener(R.id.iv_download)
            if (!item.canPlay()) {
                val errorHtml = helper.itemView.context.getString(R.string.source_error).color(Color.RED)
                helper.setText(R.id.tv_singer_album,
                        (item.author.del() + errorHtml).toHtml())
                        .setText(R.id.tv_name, item.title.del().toHtml())
            } else {
                helper.setText(R.id.tv_singer_album, item.author)
                        .setText(R.id.tv_name, item.title)
            }

            val ivPlaying = helper.getView<ImageView>(R.id.iv_playing)
            val ivAlbum = helper.getView<ImageView>(R.id.iv_album_img)
            if (helper.layoutPosition == clickPosition) {
                ivAlbum.gone()
                ivPlaying.show()
            } else {
                ivAlbum.show()
                ivPlaying.gone()
                ivAlbum?.load(item.pic)
            }
        }
    }

}