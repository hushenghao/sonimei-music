package com.dede.sonimei.module.play

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
import com.dede.sonimei.util.ImageUtil
import com.dede.sonimei.util.ScreenHelper
import com.dede.sonimei.util.extends.gone
import com.dede.sonimei.util.extends.show
import com.dede.sonimei.util.extends.toTime
import kotlinx.android.synthetic.main.fragment_play.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_play_control.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.support.v4.toast


/**
 * Created by hsh on 2018/5/23.
 */
class PlayFragment : BaseFragment(), MediaPlayer.OnPreparedListener, Runnable {

    override fun getLayoutId() = R.layout.fragment_play

    private val updateDelay = 200L

    private val mediaPlayer by lazy { MediaPlayer() }
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context!!.registerReceiver(volumeChangeReceiver, filter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iv_play.setOnClickListener(playClick)
        iv_play_bottom.setOnClickListener(playClick)

        iv_play.isClickable = false
        iv_play_bottom.isClickable = false
        sb_progress.isEnabled = false

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
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            handler.postDelayed(this, updateDelay)
            iv_play.setImageResource(R.drawable.ic_play_status)
            iv_play_bottom.setImageResource(R.drawable.ic_play_status)
        } else {
            mediaPlayer.pause()
            handler.removeCallbacks(this)
            iv_play.setImageResource(R.drawable.ic_pause_status)
            iv_play_bottom.setImageResource(R.drawable.ic_pause_status)
        }
    }

    fun playSong(song: SearchSong) {
        handler.removeCallbacks(this)

        // bottomSheet mini control
        iv_play.isClickable = false
        iv_play_bottom.isClickable = false
        sb_progress.max = 1000// 提高精度
        sb_progress.progress = 0
        sb_progress.secondaryProgress = 0
        sb_progress.isEnabled = false
        tv_name.text = song.getName()
        tv_name.isSelected = true
        tv_lrc.gone()
        GlideApp.with(this)
                .asBitmap()
                .load(song.pic)
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        iv_album_img.setImageBitmap(resource)
                        val colorDrawable = ColorDrawable(0x66000000)
                        val bitmapDrawable = BitmapDrawable(context!!.resources, ImageUtil.getPlayBitmap(context!!, resource))
                        val layerDrawable = LayerDrawable(arrayOf(bitmapDrawable, colorDrawable))
                        ll_play_content.background = layerDrawable
                    }
                })
        // control
        tv_title.text = song.title
        tv_title.isSelected = true
        tv_singer.text = song.author

        lrc_view.loadLrc(song.lrc)
        lrc_view.setOnLineChangeListener { _, lineStr, _ ->
            tv_lrc.show()
            tv_lrc.text = lineStr// update mini control lrc text
        }

        iv_download.onClick {
            DownloadHelper.download(this@PlayFragment.activity, song)
        }

        try {
            mediaPlayer.reset()
            mediaPlayer.isLooping = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                        AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                .build())
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            mediaPlayer.setDataSource(song.url)
            mediaPlayer.setOnBufferingUpdateListener { mp, percent ->
                if (percent >= 100) {
                    mp.setOnBufferingUpdateListener(null)
                }
                sb_progress.secondaryProgress = percent * 10// 更新缓冲进度条
            }
            // 准备完成后播放
            mediaPlayer.setOnPreparedListener(this)
            // 播放完回调
            mediaPlayer.setOnCompletionListener {
                iv_play.setImageResource(R.drawable.ic_pause_status)
                iv_play_bottom.setImageResource(R.drawable.ic_pause_status)
            }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                toast("在线播放错误($what:$extra)")
                false
            }
            mediaPlayer.prepareAsync()// 异步准备
        } catch (e: Exception) {
            e.printStackTrace()
            toast("在线播放错误")
        }
    }

    /**
     * 更新进度
     */
    override fun run() {
        val currentPosition = mediaPlayer.currentPosition
        val duration = mediaPlayer.duration
        lrc_view.updateTime(currentPosition.toLong())
        if (!isTouch) {
            val progress = (currentPosition.toFloat() / duration * maxProgress + .5).toInt()
            sb_progress.progress = progress
        }
        if (mediaPlayer.isPlaying) {
            handler.postDelayed(this, updateDelay)
        }
    }

    private var isTouch = false// 控制SeekBar触摸时不根据时间改变进度

    private val maxProgress = 1000f// 进度条最大进度，seek bar双进度条需要，固定最大值

    /**
     * 进度转时间
     */
    private fun progress2Time(progress: Int): Long {
        return (progress / maxProgress * mediaPlayer.duration + .5).toLong()
    }

    /**
     * 时间转进度
     */
    private fun time2Progress(time: Long): Int {
        return (time / mediaPlayer.duration.toFloat() * maxProgress + .5).toInt()
    }

    /**
     * 播放准备完成回调 implements [MediaPlayer.OnPreparedListener]
     */
    override fun onPrepared(mp: MediaPlayer) {
        val duration = mp.duration
        tv_all_time.text = duration.toTime()
        iv_play_bottom.isClickable = true
        sb_progress.isEnabled = true
        iv_play.isClickable = true
        iv_play.setImageResource(R.drawable.ic_play_status)
        iv_play_bottom.setImageResource(R.drawable.ic_play_status)

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
                val progress = seekBar!!.progress
                val time = progress2Time(progress)
                if (time <= mediaPlayer.duration) {
                    mediaPlayer.seekTo(time.toInt())
                }
            }
        })

        lrc_view.setOnPlayClickListener {
            val progress = time2Progress(it)
            sb_progress.progress = progress
            mediaPlayer.seekTo(it.toInt())
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                iv_play.setImageResource(R.drawable.ic_play_status)
                iv_play_bottom.setImageResource(R.drawable.ic_play_status)
                handler.post(this)
            }
            true
        }

        mp.start()// 准备就绪那就开始播放
        handler.post(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.setOnPreparedListener(null)// fix leak
        mediaPlayer.release()
        handler.removeCallbacks(this)
        context!!.unregisterReceiver(volumeChangeReceiver)
    }
}