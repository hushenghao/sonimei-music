package com.dede.sonimei.module.local

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.ViewOutlineProvider
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.data.local.LocalSong
import com.dede.sonimei.module.home.MainActivity
import com.dede.sonimei.module.setting.Settings.Companion.KEY_IGNORE_60S
import com.dede.sonimei.module.setting.Settings.Companion.KEY_IGNORE_PATHS
import com.dede.sonimei.util.HanziToPinyin
import com.dede.sonimei.util.extends.notNull
import com.dede.sonimei.util.extends.to
import com.tbruyelle.rxpermissions2.RxPermissions
import com.turingtechnologies.materialscrollbar.CustomIndicator
import com.turingtechnologies.materialscrollbar.ICustomAdapter
import kotlinx.android.synthetic.main.fragment_local_music.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import java.io.File

class LocalMusicFragment : BaseFragment() {

    companion object {
        const val REFRESH_LIST_ACTION = "refresh_list_action"

        fun sendBroadcast(context: Context?) {
            val intent = Intent(REFRESH_LIST_ACTION)
            context?.sendBroadcast(intent)
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadPlayList()
        }
    }

    override fun getLayoutId() = R.layout.fragment_local_music

    @SuppressLint("CheckResult")
    override fun initView(savedInstanceState: Bundle?) {
        // 重新回到栈顶时paddingTop会被置为0，原因未知
//        tool_bar.setPadding(0, ScreenHelper.getFrameTopMargin(activity), 0, 0)
        setHasOptionsMenu(true)
        activity!!.to<AppCompatActivity>().setSupportActionBar(tool_bar)

        swipe_refresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent)
        swipe_refresh.setOnRefreshListener { loadPlayList() }

        val listAdapter = LocalMusicListAdapter()
        val emptyView = LayoutInflater.from(context).inflate(R.layout.layout_local_music_empty, recycler_view_local, false)
        listAdapter.emptyView = emptyView
        recycler_view_local.adapter = listAdapter
        listAdapter.setOnItemClickListener { _, _, position ->
            val song = listAdapter.data[position]
            asActivity<MainActivity>().playSongs(listAdapter.data, song)
        }
        val customIndicator = CustomIndicator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            customIndicator.elevation = 8f
            customIndicator.outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        customIndicator.setTypeface(Typeface.SANS_SERIF)
        customIndicator.setTextSize(35)
        scroll_bar.setIndicator(customIndicator, true)

        RxPermissions(activity!!)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe({
                    if (!it) {
                        toast(R.string.permission_sd_error)
                        return@subscribe
                    }

                    loadPlayList()
                }) { it.printStackTrace() }
        val intentFilter = IntentFilter(REFRESH_LIST_ACTION)
        context?.registerReceiver(refreshReceiver, intentFilter)
    }

    private fun loadPlayList() {
        swipe_refresh.isRefreshing = true
        doAsync {
            val list = ArrayList<LocalSong>()
            val sp = defaultSharedPreferences
            val b = sp.getBoolean(KEY_IGNORE_60S, true)//忽略时间小于60s的歌曲
            val ignoreList = (sp.getStringSet(KEY_IGNORE_PATHS, null) ?: emptySet()).toList()

            fun inPath(file: File): Boolean {
                val parentPath = file.parentFile.absolutePath
                for (path in ignoreList) {
                    if (parentPath.startsWith(path)) {
                        return true
                    }
                }
                return false
            }

            fun load(cursor: Cursor?) {
                if (cursor == null) return
                val pinyin = HanziToPinyin.getInstance()
                while (cursor.moveToNext()) {
                    val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    if (b && duration < 1000 * 60) {
                        continue
                    }
                    val path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))

                    val file = File(path)
                    if (!file.isFile || !file.exists() || inPath(file)) {
                        continue
                    }

                    val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                    val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    val localSong = LocalSong(id, title, artist, album, duration, path)
                    list.add(localSong)
                    val pinYinList = pinyin.get(title)
                    if (pinYinList != null && pinYinList.size >= 1) {
                        val token = pinYinList[0]
                        if (token != null && token.type != HanziToPinyin.Token.UNKNOWN) {
                            val target = token.target
                            if (target.notNull()) {
                                localSong.key = target.substring(0, 1).toUpperCase()
                            }
                        }
                    } else {
                        localSong.key = title.substring(0, 1).toUpperCase()
                    }
                }
                cursor.close()
            }

            val strings = arrayOf(
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ARTIST
            )
            load(context!!.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    strings, null, null, null))
            load(context!!.contentResolver.query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                    strings, null, null, null))
            list.sortWith(Comparator { o1, o2 -> o1?.key?.compareTo(o2?.key ?: "#") ?: 0 })
            uiThread {
                swipe_refresh?.isRefreshing = false
                (recycler_view_local?.adapter as? LocalMusicListAdapter)?.setNewData(list)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        activity!!.to<AppCompatActivity>().setSupportActionBar(tool_bar)
    }

    override fun onDestroyView() {
        context?.unregisterReceiver(refreshReceiver)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_local_music, menu)
    }

    private inner class LocalMusicListAdapter : BaseQuickAdapter<LocalSong, BaseViewHolder>(R.layout.item_local_music), ICustomAdapter {

        override fun getCustomStringForElement(element: Int): String {
            return getItem(element)?.key ?: "#"
        }

        override fun convert(helper: BaseViewHolder?, item: LocalSong?) {
            helper?.setText(R.id.tv_name, item?.getName())
                    ?.setText(R.id.tv_singer_album, item?.album)
        }
    }
}