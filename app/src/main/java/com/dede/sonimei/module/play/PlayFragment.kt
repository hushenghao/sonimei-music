package com.dede.sonimei.module.play

import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.view.ViewPager
import android.view.View
import android.widget.SeekBar
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.component.SeekBarChangeListener
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.data.local.LocalSong
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.defaultSheep
import com.dede.sonimei.module.home.MainActivity
import com.dede.sonimei.net.GlideApp
import com.dede.sonimei.player.MusicPlayer
import com.dede.sonimei.util.ImageUtil
import com.dede.sonimei.util.ScreenHelper
import com.dede.sonimei.util.extends.hide
import com.dede.sonimei.util.extends.show
import com.dede.sonimei.util.extends.toTime
import kotlinx.android.synthetic.main.fragment_play.*
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.support.v4.toast


/**
 * Created by hsh on 2018/5/23.
 */
class PlayFragment : BaseFragment(),
        Runnable,
        MusicPlayer.OnPlayStateChangeListener,
        ServiceConnection,
        PlayListDialog.Callback,
        ILoadPlayList.OnLoadPlayListListener {

    private val updateDelay = 200L// 进度更新间隔

    private val handler = Handler()

    private val audioManager by lazy { context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    /** 监听音量变化 */
    private val volumeChangeReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    sb_volume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
            }
        }
    }

    // 播放按钮点击事件
    private val playClick = View.OnClickListener {
        if (musicBinder == null) return@OnClickListener

        if (!musicBinder!!.isPlaying) {
            musicBinder!!.start()
        } else {
            musicBinder!!.pause()
        }
    }

    // 回调中 修改背景图片，高斯模糊处理
    private val target = object : SimpleTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            val bitmapDrawable = BitmapDrawable(context!!.resources, ImageUtil.getPlayBitmap(context!!, resource))
            activity?.findViewById<View>(R.id.bottom_sheet)?.background = bitmapDrawable
        }
    }

    override fun getLayoutId() = R.layout.fragment_play

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context!!.registerReceiver(volumeChangeReceiver, filter)// 注册音量变化广播

        val intent = Intent(context, MusicService::class.java)
        context!!.startService(intent)
        context!!.bindService(intent, this, Context.BIND_AUTO_CREATE)// 绑定服务
    }

    private lateinit var playContentFragment: PlayContentFragment

    override fun initView(savedInstanceState: Bundle?) {
        iv_play.setOnClickListener(playClick)
        iv_play_bottom.setOnClickListener(playClick)

        playContentFragment = childFragmentManager.findFragmentById(R.id.fragment_play_content) as PlayContentFragment

        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        sb_volume.max = maxVolume
        sb_volume.progress = volume
        sb_volume.setOnSeekBarChangeListener(object : SeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        progress,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
            }
        })

        iv_play_mode.setOnClickListener {
            if (musicBinder == null) return@setOnClickListener

            var mode = musicBinder!!.getPlayMode()
            mode = when (mode) {
                MODE_RANDOM -> MODE_SINGLE
                MODE_SINGLE -> MODE_ORDER
                MODE_ORDER -> MODE_RANDOM
                else -> MODE_RANDOM
            }
            musicBinder!!.updatePlayMode(mode)
            iv_play_mode.setImageResource(getPlayModeDrawRes(mode))
        }

        iv_play_next.setOnClickListener {
            musicBinder?.next()
            miniControlAdapter.nextData()
        }
        iv_play_last.setOnClickListener {
            musicBinder?.last()
            miniControlAdapter.lastData()
        }

        val playListClick = View.OnClickListener {
            val listDialog = PlayListDialog(context!!, musicBinder!!.getPlayList(), musicBinder!!.getPlayIndex())
            listDialog.callback = this@PlayFragment
            listDialog.show()
        }
        iv_play_list.setOnClickListener(playListClick)
        iv_play_bottom_list.setOnClickListener(playListClick)


        // 修改title顶部距离，防止状态栏遮挡
        ll_play_content.setPadding(0, ScreenHelper.getFrameTopMargin(activity), 0, 0)

        sb_progress.setOnSeekBarChangeListener(object : SeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (musicBinder == null) return
                if (progress == 0 && !fromUser) {
                    tv_now_time.text = 0.toTime()
                    return
                }
                val time = progress2Time(progress)
                tv_now_time.text = time.toTime()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isTouch = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTouch = false
                if (musicBinder == null) return

                val progress = seekBar!!.progress
                val time = progress2Time(progress)
                if (time <= musicBinder!!.duration) {
                    musicBinder!!.currentPosition = time.toInt()
                }
            }
        })

        playContentFragment.lrcView.setOnPlayClickListener {
            if (musicBinder == null) return@setOnPlayClickListener false

            val progress = time2Progress(it)
            sb_progress.progress = progress
            musicBinder!!.currentPosition = it.toInt()
            if (!musicBinder!!.isPlaying) {
                musicBinder!!.start()
                handler.post(this)
            }
            true
        }
        onBuffer(false)
        disablePlayController()

        vp_mini_control.addOnPageChangeListener(pagerChangeListener)
    }

    private val miniControlAdapter by lazy { MiniControlAdapter(context!!, musicBinder!!) }
    /** ViewPager滑动监听处理无限轮播逻辑 */
    private val pagerChangeListener = object : ViewPager.SimpleOnPageChangeListener() {

        override fun onPageScrollStateChanged(state: Int) {
            if (state != ViewPager.SCROLL_STATE_IDLE) return

            val p = vp_mini_control.currentItem
            info("onPageSelected: $p")
            if (p == 1) return
            when (p) {
                0 -> {
                    musicBinder?.last()
                    miniControlAdapter.lastData()
                }
                2 -> {
                    musicBinder?.next()
                    miniControlAdapter.nextData()
                }
            }
            vp_mini_control.setCurrentItem(1, false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setSheep(float: Float) {
        musicBinder?.sheep(float)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getSheep(): Float {
        return musicBinder?.sheep() ?: defaultSheep
    }

    private fun enablePlayController(mp: MusicPlayer) {
        val duration = mp.duration
        tv_all_time.text = duration.toTime()
        iv_play.isClickable = true
        iv_play_bottom.isClickable = true
        sb_progress.isEnabled = true
        playContentFragment.lrcView.isEnabled = true
    }

    private fun disablePlayController() {
        tv_all_time.text = 0.toTime()
        iv_play.isClickable = false
        iv_play_bottom.isClickable = false
        sb_progress.isEnabled = false
        playContentFragment.lrcView.isEnabled = false
    }

    /** implement [PlayListDialog.Callback] */
    override fun onItemClick(index: Int, baseSong: BaseSong) {
        musicBinder!!.plays(musicBinder!!.getPlayList(), index)
    }

    override fun onItemRemove(index: Int, baseSong: BaseSong) {
        musicBinder!!.removeAt(index)
        if (musicBinder!!.getPlayList().isEmpty()) {
            val activity = activity as MainActivity
            activity.hideBottomController()// 关闭播放控制页
        }
    }

    /** implement [ServiceConnection] */

    var musicBinder: MusicBinder? = null
        private set

    override fun onServiceDisconnected(name: ComponentName?) {
        musicBinder!!.setLoadPlayListListener(null)
        musicBinder = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        musicBinder = service as MusicBinder?
        if (musicBinder == null) return

        // 绑定成功后手动调用 读取播放列表
        musicBinder!!.setLoadPlayListListener(this)
        musicBinder!!.addOnPlayStateChangeListener(this@PlayFragment)

        val mode = musicBinder!!.getPlayMode()
        iv_play_mode.setImageResource(getPlayModeDrawRes(mode))
    }

    /**
     * 播放列表加载完成
     */
    override fun onLoadFinish() {
        onDataSourceChange()
        iv_play.isClickable = true
        iv_play_bottom.isClickable = true
        if (musicBinder!!.isPlaying) {
            onPlayStart(musicBinder!!.getPlayer())
        }
    }

    /**
     * 播放音乐
     */
    fun playSongs(list: List<BaseSong>, song: BaseSong?) {
        if (musicBinder == null) return
        val indexOf = list.indexOf(song)
        musicBinder!!.plays(list, indexOf)
    }

    /**
     * 添加到播放列表
     */
    fun add2PlayList(song: BaseSong) {
        if (musicBinder == null) return
        val indexOf = musicBinder!!.getPlayList().indexOf(song)
        if (indexOf >= 0) {
            toast(R.string.toast_has_in_list)
            return
        }
        musicBinder!!.add(song)
    }

    /**
     * 更新进度 implement [Runnable]
     */
    override fun run() {
        val currentPosition = musicBinder!!.currentPosition
        val duration = musicBinder!!.duration
        if (!bottomSheetOpen) {
            val lrcLine = playContentFragment.findLine(currentPosition.toLong())
            miniControlAdapter.updateLrc(lrc = lrcLine)
        } else {
            playContentFragment.updateTime(currentPosition.toLong())
        }
        if (!isTouch) {
            val progress = (currentPosition.toFloat() / duration * maxProgress + .5).toInt()
            sb_progress.progress = progress
        }
        if (musicBinder!!.isPlaying) {
            handler.postDelayed(this, updateDelay)
        }
    }

    private var isTouch = false// 控制SeekBar触摸时不根据时间改变进度

    private val maxProgress = 1000f// 进度条最大进度，seek bar双进度条需要，固定最大值

    /**
     * 进度转时间
     */
    private fun progress2Time(progress: Int): Long {
        return (progress / maxProgress * musicBinder!!.duration + .5).toLong()
    }

    /**
     * 时间转进度
     */
    private fun time2Progress(time: Long): Int {
        return (time / musicBinder!!.duration.toFloat() * maxProgress + .5).toInt()
    }

    private var bottomSheetOpen = false// 首页播放控制布局是否打开

    /**
     * 首页滑动状态变化回调
     */
    fun onBottomSheetSlideOffsetChange(slideOffset: Float) {
        bottomSheetOpen = slideOffset > .75
        if (bottomSheetOpen) {
            if (musicBinder?.isPlaying == true) {
                playContentFragment.playAnim(true)
            }
            ll_bottom_play.hide()
        } else {
            playContentFragment.playAnim(false)
            ll_bottom_play.show()
        }

        var b = 1 - slideOffset * 2f
        if (b < 0f) b = 0f
        ll_bottom_play.alpha = b
    }

    /**
     * 播放状态改变监听 implements [MusicPlayer.onPlayStateChangeListeners]
     */
    override fun onPlayStop() {
        iv_play.setImageResource(R.drawable.ic_pause_status)
        iv_play_bottom.setImageResource(R.drawable.ic_pause_status)
        playContentFragment.playAnim(false)
        handler.removeCallbacks(this)
    }

    override fun onPlayStart(mp: MusicPlayer) {
        enablePlayController(mp)

        playContentFragment.playAnim(true)
        iv_play.setImageResource(R.drawable.ic_play_status)
        iv_play_bottom.setImageResource(R.drawable.ic_play_status)

        handler.post(this)
    }

    override fun onPlayPause() {
        onPlayStop()
    }

    override fun onCompletion() {
        onPlayStop()
    }

    override fun onPrepared(mp: MusicPlayer) {
    }

    override fun onDataSourceChange() {
        handler.removeCallbacks(this)
        sb_progress.max = maxProgress.toInt()// 提高精度
        sb_progress.progress = 0
        sb_progress.secondaryProgress = 0

        val resource = BitmapFactory.decodeResource(resources, R.drawable.icon_play_bg_normal)
        activity?.findViewById<View>(R.id.bottom_sheet)?.background =
                BitmapDrawable(resources, ImageUtil.getPlayBitmap(context!!, resource))

        val song = musicBinder!!.getPlayInfo()
        if (vp_mini_control.adapter == null) {
            vp_mini_control.adapter = miniControlAdapter
        }
        miniControlAdapter.refresh()
        vp_mini_control.setCurrentItem(1, false)
        if (song != null) {
            tv_title.text = song.title
            tv_title.isSelected = true
            playContentFragment.setSongInfo(song)
            val i = dip(45f)
            if (song is SearchSong) {
                GlideApp.with(this)
                        .asBitmap()
                        .error(R.drawable.ic_music)
                        .transform(RoundedCorners(dip(2)))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(i, i)
                        .load(song.pic)
                        .into<SimpleTarget<Bitmap>>(target)
                tv_singer.text = song.author
            } else if (song is LocalSong) {
                GlideApp.with(this)
                        .asBitmap()
                        .transform(RoundedCorners(dip(2)))
                        .override(i, i)
                        .load(song.picByteArray())
                        .into<SimpleTarget<Bitmap>>(target)
                tv_singer.text = song.author
            }
            (activity as MainActivity).showBottomController()
        }
    }

    override fun onBuffer(inBuffer: Boolean) {
        val layerDrawable = sb_progress.thumb as LayerDrawable
        val drawable = layerDrawable.findDrawableByLayerId(R.id.drawable_rotate)
        if (inBuffer) {
            // 进度条没有变化，缓冲卡顿
            drawable.alpha = 255
            (drawable as Animatable).start()
        } else {
            // 进度条变化，正在缓冲
            drawable.alpha = 0
        }
    }

    override fun onBufferUpdate(percent: Int) {
        sb_progress.secondaryProgress = percent * 10// 更新缓冲进度条
    }

    override fun onDestroyView() {
        vp_mini_control.removeOnPageChangeListener(pagerChangeListener)
        super.onDestroyView()
    }

    override fun onDestroy() {
        musicBinder?.removeOnPlayStateChangeListener(this)
        musicBinder?.setLoadPlayListListener(null)
        context?.unbindService(this)
        if (musicBinder?.isPlaying == false) {
            context?.stopService(Intent(context, MusicService::class.java))
        }
        handler.removeCallbacks(this)
        context?.unregisterReceiver(volumeChangeReceiver)
        super.onDestroy()
    }
}