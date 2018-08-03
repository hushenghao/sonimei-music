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
import android.view.ViewGroup
import android.widget.SeekBar
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.component.SeekBarChangeListener
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
class PlayFragment : BaseFragment(), Runnable, MusicPlayer.OnPlayStateChangeListener {

    override fun getLayoutId() = R.layout.fragment_play

    private val updateDelay = 200L

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context!!.registerReceiver(volumeChangeReceiver, filter)

        val intent = Intent(context, MusicService::class.java)
        context?.startService(intent)
        connection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                musicBinder = null
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                musicBinder = service as MusicService.MusicBinder?
                if (musicBinder == null) return

                musicBinder!!.addOnStateChangeListener(this@PlayFragment)

                val songInfo = musicBinder!!.getPlaySongInfo()
                if (songInfo != null && songInfo is SearchSong) {
                    playSong(songInfo)// 恢复状态
                }
            }
        }
        context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
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

        iv_close.onClick { (activity as MainActivity?)?.toggleBottomSheet() }

        // 修改title顶部距离，防止状态栏遮挡
        val params = tv_title.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = params.topMargin + ScreenHelper.getFrameTopMargin(activity)

        lrc_view.setOnLineChangeListener { _, lineStr, _ ->
            tv_lrc.show()
            tv_lrc.text = lineStr// update mini control lrc text
        }

        sb_progress.setOnSeekBarChangeListener(object : SeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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

        iv_play.isClickable = false
        iv_play_bottom.isClickable = false
        sb_progress.isEnabled = false
        lrc_view.isEnabled = false
    }

    private val audioManager by lazy { context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    /**
     * 监听音量变化
     */
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
            handler.postDelayed(this, updateDelay)
        } else {
            musicBinder!!.pause()
            handler.removeCallbacks(this)
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

    private var musicBinder: MusicService.MusicBinder? = null
    private var connection: ServiceConnection? = null

    fun playSong(song: SearchSong) {
        handler.removeCallbacks(this)

        // bottomSheet mini control
        sb_progress.max = maxProgress.toInt()// 提高精度
        sb_progress.progress = 0
        sb_progress.secondaryProgress = 0
        tv_name.text = song.getName()
        tv_name.isSelected = true
        tv_lrc.gone()
        GlideApp.with(this)
                .asBitmap()
                .load(song.pic)
                .into<SimpleTarget<Bitmap>>(target)
        // control
        tv_title.text = song.title
        tv_title.isSelected = true
        tv_singer.text = song.author

        lrc_view.loadLrc(song.lrc)

        iv_download.onClick {
            DownloadHelper.download(this@PlayFragment.activity, song)
        }

        if (musicBinder != null) {
            if (musicBinder!!.isPlaying && song == musicBinder!!.getPlaySongInfo()) {
                onPlayStart(musicBinder!!.getMusicPlayer())
            } else {
                musicBinder!!.play(song)
            }
            musicBinder!!.isLooping = true
        }
    }

    /**
     * 更新进度
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
    }

    override fun onPlayStart(mp: MusicPlayer) {
        val duration = mp.duration
        tv_all_time.text = duration.toTime()
        iv_play.isClickable = true
        iv_play_bottom.isClickable = true
        sb_progress.isEnabled = true
        lrc_view.isEnabled = true

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

    override fun onBufferUpdate(percent: Int) {
        sb_progress.secondaryProgress = percent * 10// 更新缓冲进度条
    }

    override fun onDestroy() {
        musicBinder?.removeOnStateChangeListener(this)
        if (musicBinder?.isPlaying == false) {
            context?.stopService(Intent(context, MusicService::class.java))
        }
        context?.unbindService(connection)
        handler.removeCallbacks(this)
        context?.unregisterReceiver(volumeChangeReceiver)
        super.onDestroy()
    }
}