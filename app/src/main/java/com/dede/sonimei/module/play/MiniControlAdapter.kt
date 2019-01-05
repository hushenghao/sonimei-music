package com.dede.sonimei.module.play

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.dede.sonimei.R
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.data.local.LocalSong
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.home.MainActivity
import com.dede.sonimei.net.GlideApp
import com.dede.sonimei.util.extends.isNull
import com.dede.sonimei.util.extends.show
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.collections.forEach
import org.jetbrains.anko.dip
import org.jetbrains.anko.info

/**
 * 播放页底部ViewPager的Adapter
 */
class MiniControlAdapter(val context: Context, private val musicBinder: MusicBinder) : PagerAdapter(), AnkoLogger {

    private inner class PagerHolder(view: View) {
        val img = view.findViewById<ImageView>(R.id.iv_album_img)!!
        val title = view.findViewById<TextView>(R.id.tv_name)!!
        val lrc = view.findViewById<TextView>(R.id.tv_lrc)!!
    }

    private val data = ArrayList<BaseSong>(3)
    private val childViews = SparseArray<View>(3)

    var playIndex: Int = 0
        private set

    init {
        switch(true, false)
    }

    fun nextData() {
        val temp = ArrayList<View>(3)
        childViews.forEach { _, view ->
            temp.add(view)
        }
        if (temp.size == 3) {
            temp.add(1, temp.removeAt(2))//把最后一个移动到中间
            childViews.clear()
            for (i in (0..2)) {
                childViews.put(i, temp[i])
            }
        }
        switch(true)
    }

    fun lastData() {
        val temp = ArrayList<View>(3)
        childViews.forEach { _, view ->
            temp.add(view)
        }
        if (temp.size == 3) {
            temp.add(1, temp.removeAt(0))//把第一个移动到中间
            childViews.clear()
            for (i in (0..2)) {
                childViews.put(i, temp[i])
            }
        }
        switch(false)
    }

    private var refresh = false

    fun refresh() {
        val list = musicBinder.getPlayList()
        playIndex = musicBinder.getPlayIndex()
        val playSong = list[playIndex]
        val service = musicBinder.service
        val lastIndex = service.lastIndex()
        val nextIndex = service.nextIndex()

        val lastSong = list[lastIndex]
        val nextSong = list[nextIndex]

        val temp = arrayListOf(lastSong, playSong, nextSong)
        if (temp == data) {
            return
        }
        refresh = true
        info("refresh: $lastIndex : $playIndex : $nextIndex")
        data.clear()
        data.addAll(temp)

        notifyDataSetChanged()
    }

    private fun switch(isNext: Boolean, notify: Boolean = true) {
        data.clear()

        val list = musicBinder.getPlayList()
        playIndex = musicBinder.getPlayIndex()
        val playSong = list[playIndex]
        val service = musicBinder.service
        val nextIndex: Int
        val lastIndex: Int
        if (isNext) {
            nextIndex = service.nextIndex()
            lastIndex = service.lastIndex()
        } else {
            lastIndex = service.lastIndex()
            nextIndex = service.nextIndex()
        }
        info("switch: $isNext    $lastIndex : $playIndex : $nextIndex")
        val nextSong = list[nextIndex]
        val lastSong = list[lastIndex]

        data.add(lastSong)
        data.add(playSong)
        data.add(nextSong)

        if (notify) notifyDataSetChanged()
    }

    fun updateLrc(lrc: String? = null) {
        if (lrc.isNull()) return
        val holder = childViews[1].tag as PagerHolder
        holder.lrc.show()
        holder.lrc.text = lrc
    }

    private fun getView(container: ViewGroup, position: Int): View {
        var view = childViews.get(position)
        if (view == null) {
            view = LayoutInflater.from(container.context)
                    .inflate(R.layout.layout_bottom_sheet_play_control, container, false)
            view!!.setOnClickListener { (context as MainActivity).toggleBottomSheet() }
            childViews.put(position, view)
        }
        container.addView(view)
        return view
    }

    override fun isViewFromObject(p0: View, p1: Any) = p0 == p1

    private var refreshCount = 0

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = getView(container, position)
        val holder = if (view.tag == null) {
            PagerHolder(view).also { view.tag = it }
        } else {
            view.tag as PagerHolder
        }
        val song = data[position]

        if (song is SearchSong) {
            holder.lrc.text = song.author
        } else if (song is LocalSong) {
            holder.lrc.text = song.author
        }
        holder.title.text = song.title
        if (position != 1 || holder.img.drawable == null || refresh) {// 除了中间的其他都重新绑定数据
            refreshCount++
            if (song is SearchSong) {
                GlideApp.with(context)
                        .load(song.pic)
                        .error(R.drawable.ic_music)
                        .placeholder(holder.img.drawable)
                        .transform(RoundedCorners(context.dip(2)))
                        .into(holder.img)
            } else if (song is LocalSong) {
                GlideApp.with(context)
                        .load(song.picByteArray())
                        .placeholder(holder.img.drawable)
                        .error(R.drawable.ic_music)
                        .transform(RoundedCorners(context.dip(2)))
                        .into(holder.img)
            }
        }
        if (refreshCount == 3) {
            refresh = false
        }
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount() = 3

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }
}