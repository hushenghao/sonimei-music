package com.dede.sonimei.module.home

import android.os.Bundle
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
import com.dede.sonimei.util.extends.kson.fromJson
import com.dede.sonimei.util.extends.load
import com.dede.sonimei.util.extends.notNull
import kotlinx.android.synthetic.main.fragment_search_result.*
import org.jetbrains.anko.info
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

    @MusicSource
    private var source = NETEASE
    private var searchText: String? = null

    private val presenter by lazy { SearchPresenter(this) }

    // 列表适配器
    private lateinit var adapter: BaseQuickAdapter<SearchSong, BaseViewHolder>

    override fun getLayoutId() = R.layout.fragment_search_result

    override fun initView(savedInstanceState: Bundle?) {
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent)
        swipe_refresh.setOnRefreshListener {
            if (searchText.notNull())
                presenter.search(searchText!!, source)
            else
                hideLoading()
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
        adapter.setOnItemChildClickListener { _, view, position ->
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
        adapter.setEmptyView(R.layout.layout_search_empty)

//        adapter.setNewData(json.fromJson())
//        adapter.loadMoreEnd()
    }

    val json = "[{\n" +
            "            \"type\": \"netease\",\n" +
            "            \"link\": \"http://music.163.com/#/song?id=32835565\",\n" +
            "            \"songid\": 32835565,\n" +
            "            \"title\": \"国王与乞丐\",\n" +
            "            \"author\": \"华晨宇,杨宗纬\",\n" +
            "            \"lrc\": \"[by:立酱]\\n[00:00.00] 作曲 : Mike Chan/Faizal Tahir\\n[00:00.666] 作词 : 代岳东\\n[00:02.00]编曲：郑楠\\n[00:03.00]\\n[00:06.10]华：怎么了 怎么了\\n[00:17.25]一份爱失去了光泽\\n[00:20.38]面对面 背对背\\n[00:23.60]反复挣扎怎么都痛\\n[00:27.01]以为爱坚固像石头\\n[00:30.32]谁知一秒钟就碎落\\n[00:33.69]难道心痛都要不断打磨\\n[00:38.14]纬：抱紧你的我比国王富有\\n[00:46.43]曾多么快乐\\n[00:50.48]华：失去你的我比乞丐落魄\\n[00:59.64]痛多么深刻\\n[01:05.98]噢 喔 噢 喔\\n[01:11.13]噢 喔 噢 喔\\n[01:16.16]纬：谁哭着谁笑着\\n[01:22.05]一人分饰两个角色\\n[01:25.30]越执迷越折磨\\n[01:28.60]回忆还在煽风点火\\n[01:32.10]明知往前就会坠落\\n[01:35.17]抱着遗憾重返寂寞\\n[01:38.43]爱到最后究竟还剩什么\\n[01:43.88]纬：抱紧你的我比国王富有\\n[01:51.66]曾多么快乐\\n[01:56.41]华：失去你的我比乞丐落魄\\n[02:04.43]痛多么深刻\\n[02:12.30]当一切 结束了 安静了 过去了\\n[02:17.77]为什么 还拥有 一万个 舍不得\\n[02:24.34]合：喔 喔\\n[02:37.07]谁又能感受\\n[02:42.73]回忆里的我比国王富有\\n[02:50.04]奢侈的快乐\\n[02:55.76]失去你以后比乞丐落魄\\n[03:06.05]心痛如刀割\\n[03:13.23]怀念那时你安静陪着我\\n[03:17.48]噢 噢\\n[03:19.77]柔软时光里最美的挥霍\\n[03:25.87]喔 喔\\n[03:29.54]爱有多快乐\\n[03:33.89]痛有多深刻\\n[03:40.51]痛有多深刻\\n[03:43.20]\\n[03:43.30]制作人：郑楠\\n[03:43.40]制作助理：王子\\n[03:43.50]配唱制作人：翁乙仁\\n[03:43.60]录音：刘灵\\n[03:43.70]吉他：牛子健\\n[03:43.80]鼓：贝贝\\n[03:43.90]贝斯：韩阳\\n[03:43.92]和声编写 / 和声：余昭源\\n[03:43.94]混音：Craig Burbidge\\n[03:43.96]弦乐：国际首席爱乐乐团\\n[03:43.97]弦乐编写：郑楠\\n[03:43.98]录音棚：Big J Studio & TweakToneLabs\\n[03:44.40]词OP：上海天娱传媒有限公司\\n[03:45.70]曲OP：上海天娱传媒有限公司\\n\",\n" +
            "            \"url\": \"http://music.163.com/song/media/outer/url?id=32835565.mp3\",\n" +
            "            \"pic\": \"http://p1.music.126.net/UsSAd3Bdf77DjhCuTSEvUw==/109951163077613693.jpg?param=300x300\"\n" +
            "        }]"

    fun search(search: String?, @MusicSource source: Int) {
        if (search.isNull()) return
        if (!userVisibleHint || !isVisible) return

        this.source = source
        this.searchText = search
        info(sourceName(source) + " 搜索： " + this.searchText)
        presenter.search(this.searchText!!, source)
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