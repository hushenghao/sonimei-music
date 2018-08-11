package com.dede.sonimei.module.play

import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.View
import android.widget.SeekBar
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.component.SeekBarChangeListener
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.download.DownloadHelper
import com.dede.sonimei.module.home.MainActivity
import com.dede.sonimei.net.GlideApp
import com.dede.sonimei.player.MusicPlayer
import com.dede.sonimei.util.ImageUtil
import com.dede.sonimei.util.ScreenHelper
import com.dede.sonimei.util.extends.gone
import com.dede.sonimei.util.extends.show
import com.dede.sonimei.util.extends.toTime
import kotlinx.android.synthetic.main.fragment_play.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_play_control.*
import org.jetbrains.anko.sdk25.coroutines.onClick


/**
 * Created by hsh on 2018/5/23.
 */
class PlayFragment : BaseFragment(), Runnable, MusicPlayer.OnPlayStateChangeListener,
        ServiceConnection, PlayListDialog.Callback {

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
            iv_album_img.setImageBitmap(resource)
            val colorDrawable = ColorDrawable(0x66000000)
            val bitmapDrawable = BitmapDrawable(context!!.resources, ImageUtil.getPlayBitmap(context!!, resource))
            val layerDrawable = LayerDrawable(arrayOf(bitmapDrawable, colorDrawable))
            ll_play_content.background = layerDrawable
        }
    }

    override fun getLayoutId() = R.layout.fragment_play

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context!!.registerReceiver(volumeChangeReceiver, filter)// 注册音量变化广播

        val intent = Intent(context, MusicService::class.java)
        context?.startService(intent)
        context?.bindService(intent, this, Context.BIND_AUTO_CREATE)// 绑定服务
    }

    override fun initView(savedInstanceState: Bundle?) {
        iv_play.setOnClickListener(playClick)
        iv_play_bottom.setOnClickListener(playClick)

        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        sb_volume.max = maxVolume
        sb_volume.progress = volume
        sb_volume.setOnSeekBarChangeListener(object : SeekBarChangeListener() {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        seekBar!!.progress,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
            }
        })

        iv_play_mode.onClick {
            if (musicBinder == null) return@onClick

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

        iv_play_next.onClick { musicBinder?.next() }

        iv_play_last.onClick { musicBinder?.last() }


        val playListClick = View.OnClickListener {
            val listDialog = PlayListDialog(context!!, musicBinder!!.getPlayList(), musicBinder!!.getPlayIndex())
            listDialog.callback = this@PlayFragment
            listDialog.show()
        }
        iv_play_list.setOnClickListener(playListClick)
        iv_play_bottom_list.setOnClickListener(playListClick)


        // 修改title顶部距离，防止状态栏遮挡
        ll_play_content.setPadding(0, ScreenHelper.getFrameTopMargin(activity), 0, 0)
        iv_download.onClick {
            val song = musicBinder?.getPlayInfo() as? SearchSong ?: return@onClick
            DownloadHelper.download(activity, song)
        }

        lrc_view.setOnLineChangeListener { _, lineStr, _ ->
            tv_lrc.show()
            tv_lrc.text = lineStr// update mini control lrc text
        }

        sb_progress.setOnSeekBarChangeListener(object : SeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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

        lrc_view.setOnPlayClickListener {
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

        disablePlayController()
    }

    private fun enablePlayController(mp: MusicPlayer) {
        val duration = mp.duration
        tv_all_time.text = duration.toTime()
        iv_play.isClickable = true
        iv_play_bottom.isClickable = true
        sb_progress.isEnabled = true
        lrc_view.isEnabled = true
    }

    private fun disablePlayController() {
        tv_all_time.text = 0.toTime()
        iv_play.isClickable = false
        iv_play_bottom.isClickable = false
        sb_progress.isEnabled = false
        lrc_view.isEnabled = false
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

    private var musicBinder: MusicBinder? = null

    override fun onServiceDisconnected(name: ComponentName?) {
        musicBinder = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        musicBinder = service as MusicBinder?
        if (musicBinder == null) return

        // 未播放状态，读取播放列表
        musicBinder!!.onLoadPlayListFinishListener = object : MusicBinder.OnLoadPlayListFinishListener {
            override fun onFinish() {
                musicBinder?.onLoadPlayListFinishListener = null
                onDataSourceChange()
                iv_play.isClickable = true
                iv_play_bottom.isClickable = true
            }
        }
        musicBinder!!.addOnPlayStateChangeListener(this@PlayFragment)

        if (musicBinder!!.isPlaying) {
            onDataSourceChange()
            onPlayStart(musicBinder!!.getPlayer())
        } else {
            if (musicBinder!!.getPlayList().isNotEmpty()) {
                onDataSourceChange()
                iv_play.isClickable = true
                iv_play_bottom.isClickable = true
            }
        }
        val mode = musicBinder!!.getPlayMode()
        iv_play_mode.setImageResource(getPlayModeDrawRes(mode))
    }

    /**
     * 播放音乐
     */
    fun playSongs(list: List<BaseSong>, song: BaseSong?) {
        if (musicBinder == null) return
        val indexOf = list.indexOf(song)
        musicBinder!!.plays(list, indexOf)
    }

    fun playSongs(list: List<BaseSong>, index: Int) {
        if (musicBinder == null) return
        musicBinder!!.plays(list, index)
    }

    /**
     * 更新进度 implement [Runnable]
     */
    override fun run() {
        val currentPosition = musicBinder!!.currentPosition
        val duration = musicBinder!!.duration
        lrc_view.updateTime(currentPosition.toLong())
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

    /**
     * 播放状态改变监听 implements [MusicPlayer.onPlayStateChangeListeners]
     */
    override fun onPlayStop() {
        iv_play.setImageResource(R.drawable.ic_pause_status)
        iv_play_bottom.setImageResource(R.drawable.ic_pause_status)

        handler.removeCallbacks(this)
    }

    override fun onPlayStart(mp: MusicPlayer) {
        enablePlayController(mp)

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

        val song = musicBinder?.getPlayInfo()
        if (song != null) {
            tv_name.text = song.getName()
            tv_name.isSelected = true
            tv_lrc.gone()
            tv_title.text = song.title
            tv_title.isSelected = true
            if (song is SearchSong) {
                GlideApp.with(this)
                        .asBitmap()
                        .load(song.pic)
                        .into<SimpleTarget<Bitmap>>(target)
                tv_singer.text = song.author
                lrc_view.loadLrc(song.lrc)
                iv_download.show()
            } else {
                iv_download.gone()
            }
            (activity as MainActivity).showBottomController()
        }
    }

    override fun onBufferUpdate(percent: Int) {
        sb_progress.secondaryProgress = percent * 10// 更新缓冲进度条
    }


    override fun onDestroy() {
        musicBinder?.removeOnPlayStateChangeListener(this)
        if (musicBinder?.isPlaying == false) {
            context?.stopService(Intent(context, MusicService::class.java))
        }
        context?.unbindService(this)
        handler.removeCallbacks(this)
        context?.unregisterReceiver(volumeChangeReceiver)
        super.onDestroy()
    }
}