package com.dede.sonimei.module.play

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.dede.sonimei.R
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.player.MusicPlayer
import com.dede.sonimei.util.extends.*
import org.jetbrains.anko.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by hsh on 2018/8/2.
 */
class MusicService : Service(), IPlayControllerListenerI,
        AudioManager.OnAudioFocusChangeListener, AnkoLogger {

    /** implement [IPlayControllerListenerI] */

    override fun getPlayList(): List<BaseSong> = ArrayList(playList)

    override fun removeAt(index: Int) {
        if (index >= playList.size || index < 0) return

        if (index < playIndex) {
            playList.removeAt(index)
            playIndex--// 调整索引
        } else {
            playList.removeAt(index)
            // 移除当前播放的位置
            if (index == playIndex) {
                if (playList.isEmpty()) {
                    playIndex = 0
                    musicPlayer.stop()
                } else {
                    playIndex = index
                    val end = playList.size - 1
                    if (playIndex > end) {
                        playIndex = end
                    }
                    musicPlayer.stop()
                    autoStart = false
                    play(playList[playIndex])
                }
            }
        }
        savePlayList()
    }

    override fun remove(song: BaseSong?) {
        val indexOf = playList.indexOf(song)
        removeAt(indexOf)
    }

    override fun add(song: BaseSong?, index: Int) {
        if (song == null) return
        if (index > 0) {
            if (index <= playIndex) {
                playIndex++
            }
            playList.add(index, song)
        } else {
            playList.add(song)
        }
        if (playList.size == 1) {
            playIndex = 0
            play(song)
        }
        savePlayList()
    }

    override fun clear() {
        playList.clear()
        playIndex = 0
        musicPlayer.stop()
    }

    override fun play(song: BaseSong?) {
        if (song != null && song.path.notNull()) {
            val indexOf = this.playList.indexOf(song)
            if (indexOf == -1) {
                this.playList.add(song)
                this.playIndex = this.playList.size - 1

                savePlayList()
            } else {
                this.playIndex = indexOf
            }
            play(song.path!!)
        }
    }

    override fun plays(playList: List<BaseSong>?, index: Int) {
        if (playList == null || playList.isEmpty()) return

        if (this.playList != playList) {
            this.playList.clear()
            this.playList.addAll(playList)

            savePlayList()
        }

        if (index > 0 && index < this.playList.size) {
            playIndex = index
        } else {
            playIndex = 0
        }

        when (playMode) {
            MODE_SINGLE, MODE_ORDER -> {
                playIndex--// 在next中++后值不变
                next()
            }
            MODE_RANDOM -> {
                val song = this.playList[playIndex]
                if (song.path.notNull()) {
                    play(song.path!!)
                } else {
                    toast(R.string.play_path_empty)
                }
            }
        }
    }

    /**
     * 播放
     */
    private fun play(path: String) {
        info("index:" + playIndex + "  path:" + path)
        try {
            val looping = this.musicPlayer.isLooping
            this.musicPlayer.reset()
            this.musicPlayer.isLooping = looping
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.musicPlayer.setAudioAttributes(AudioAttributes.Builder()
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build())
            } else {
                this.musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            this.musicPlayer.setDataSource(path)
            this.musicPlayer.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun start() {
        requestAudioFocus()

        if (musicPlayer.isAsyncPrepared) {
            musicPlayer.start()
        } else {
            plays(playList, playIndex)
        }
    }

    override fun pause() {
        musicPlayer.pause()
    }

    override fun next() {
        var path: String? = null
        val size = playList.size
        if (size == 0) return
        if (size == 1) {
            path = playList[0].path
        } else {
            when (playMode) {
                MODE_ORDER, MODE_SINGLE -> {
                    var i = playIndex
                    do {
                        i++
                        if (i >= size) {
                            i = 0
                        }
                        path = playList[i].path
                    } while (path.isNull() && i != playIndex)// 循环了一遍，直接break
                    playIndex = i
                }
                MODE_RANDOM -> {
                    var i: Int
                    do {
                        i = random.nextInt(size)
                        path = playList[i].path
                    } while (path.isNull() && i != playIndex)
                    playIndex = i
                }
            }
        }
        if (path.isNull()) {
            toast(R.string.play_path_empty)
            return
        }

        play(path!!)
    }

    override fun last() {
        var path: String? = null
        val size = playList.size
        if (size == 0) return
        if (size == 1) {
            path = playList[0].path
        } else {
            when (playMode) {
                MODE_ORDER, MODE_SINGLE -> {
                    var i = playIndex
                    do {
                        i--
                        if (i < 0) {
                            i = size - 1
                        }
                        path = playList[i].path
                    } while (path.isNull() && i != playIndex)// 循环了一遍，直接break
                    playIndex = i
                }
                MODE_RANDOM -> {
                    var i: Int
                    do {
                        i = random.nextInt(size)
                        path = playList[i].path
                    } while (path.isNull() && i != playIndex)
                    playIndex = i
                }
            }
        }
        if (path.isNull()) {
            toast(R.string.play_path_empty)
            return
        }

        play(path!!)
    }

    override fun getPlayInfo(): BaseSong? {
        return if (playIndex >= 0 && playIndex < playList.size) {
            playList[playIndex]
        } else {
            null
        }
    }

    override fun updatePlayMode(@PlayMode mode: Int) {
        if (mode < MODE_ORDER || mode > MODE_RANDOM) return

        this.playMode = mode
        toast(getPlayModeStrRes(this.playMode))
        musicPlayer.isLooping = this.playMode == MODE_SINGLE// 是否是单曲循环

        sp.edit().putInt("play_mode", playMode).apply()
    }

    @PlayMode
    override fun getPlayMode(): Int {
        return this.playMode
    }

    override fun getPlayIndex(): Int {
        return playIndex
    }

    override fun addOnPlayStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?) {
        musicPlayer.addOnPlayStateChangeListener(listener)
    }

    override fun removeOnPlayStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?) {
        musicPlayer.removeOnPlayStateChangeListener(listener)
    }

    /** ================== */

    private lateinit var musicPlayer: MusicPlayer

    private val playList = ArrayList<BaseSong>()// 播放列表
    private var playIndex = 0// 播放索引

    @PlayMode
    private var playMode: Int = MODE_ORDER// 播放模式，默认顺序播放
    /** 随机播放取随机数 */
    private val random = Random()

    private var autoStart = true

    /** 处理音频焦点 */
    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE).to<AudioManager>()
    }

    private val sp by lazy { applicationContext.defaultSharedPreferences }

    /**
     * 获取焦点
     */
    private fun requestAudioFocus(): Boolean {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setOnAudioFocusChangeListener(this)
                            .setWillPauseWhenDucked(true)
                            .setAudioAttributes(
                                    AudioAttributes.Builder()
                                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                            .build()
                            )
                            .build()
            )
        } else {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 释放焦点
     */
    private fun releaseAudioFocus(): Boolean {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .build()
            )
        } else {
            audioManager.abandonAudioFocus(this)
        }
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    // 短暂失去焦点时，恢复焦点后自动播放
    private var onResumeFocusAutoStart = false

    /**
     * 焦点变化回调 implement [AudioManager.OnAudioFocusChangeListener]
     */
    override fun onAudioFocusChange(focusChange: Int) {
        if (!musicPlayer.hasDataSource || !musicPlayer.isAsyncPrepared) {
            return
        }
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!musicPlayer.isPlaying && onResumeFocusAutoStart) {
                    start()
                    onResumeFocusAutoStart = false
                    musicPlayer.setVolume(1f, 1f)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (musicPlayer.isPlaying) {
                    onResumeFocusAutoStart = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (musicPlayer.isPlaying) {
                    musicPlayer.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (musicPlayer.isPlaying) {
                    musicPlayer.setVolume(0.1f, 0.1f)
                }
            }
        }
    }

    fun getMusicPlayer(): MusicPlayer {
        return musicPlayer
    }

    override fun onCreate() {
        super.onCreate()
        playIndex = sp.getInt("play_index", 0)
        playMode = sp.getInt("play_mode", MODE_ORDER)
        loadPlayList()

        initMusicPlayer()

        // 注册耳机断开广播
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(headsetPlugReceiver, intentFilter)
    }

    /**
     * 接收耳机断开广播，包括蓝牙
     */
    private val headsetPlugReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == action) {
                info("Headset Disconnected 暂停播放")
                musicPlayer.pause()
            }
        }
    }

    /**
     * 初始化MediaPlayer
     */
    private fun initMusicPlayer() {
        musicPlayer = MusicPlayer()
        musicPlayer.setOnErrorListener { mp, what, extra ->
            when (what) {
                MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                    musicPlayer = (mp as MusicPlayer).getRecreateHelper().recreate()
                    initMusicPlayer()
                    return@setOnErrorListener true
                }
                MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                    toast(getString(R.string.play_error, extra))
                    next()
                    return@setOnErrorListener true
                }
                -38 -> {
                    // getDuration error
                    return@setOnErrorListener true
                }
            }
            false
        }

        musicPlayer.addOnPlayStateChangeListener(object : MusicPlayer.SimplePlayStateChangeListener() {
            override fun onPrepared(mp: MusicPlayer) {
                if (autoStart) {
                    start()// 准备完成自动播放
                } else {
                    autoStart = true
                }
            }

            override fun onCompletion() {
                next()// 播放下一曲
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private var binder: MusicBinder? = null

    override fun onBind(intent: Intent?): IBinder {
        binder = MusicBinder(this)
        return binder!!
    }

    override fun onUnbind(intent: Intent?): Boolean {
        savePlayList()
        return super.onUnbind(intent)
    }


    override fun onDestroy() {
        unregisterReceiver(headsetPlugReceiver)
        musicPlayer.release()
        releaseAudioFocus()
        sp.edit().putInt("play_index", playIndex).apply()

        super.onDestroy()
    }

    /** ======== 播放状态保存 ======= */

    private fun savePlayList() {
        doAsync({ it.printStackTrace() }) {
            playList.save(File(filesDir, "play_list"))
        }
    }

    private fun loadPlayList() {
        doAsync({
            it.printStackTrace()
            Log.i("hahah","==========================")
            Log.i("hahah",Thread.currentThread().name)
        }) {
            val list = File(filesDir, "play_list").load<List<BaseSong>>()
            if (list != null && list.isNotEmpty()) {
                uiThread {
                    playList.clear()
                    playList.addAll(list)
                    val size = playList.size
                    if (this@MusicService.playIndex >= size) {
                        this@MusicService.playIndex = size - 1
                    }
                    binder?.onLoadPlayListFinishListener?.onFinish()
                }
            }
        }
    }

}