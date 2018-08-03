package com.dede.sonimei.module.play

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.player.MusicPlayer
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.info


/**
 * Created by hsh on 2018/8/2.
 */
class MusicService : Service(), AnkoLogger {

    private lateinit var musicPlayer: MusicPlayer

    override fun onCreate() {
        super.onCreate()
        initMusicPlayer()

        // 耳机断开广播
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(headsetPlugReceiver, intentFilter)
    }

    private val headsetPlugReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == action) {
                info("Headset Disconnected 暂停播放")
                musicPlayer.pause()
            }
        }
    }

    private fun initMusicPlayer() {
        musicPlayer = MusicPlayer()
        musicPlayer.autoManagerAudioFocus(this, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            musicPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build())
        } else {
            musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }

        musicPlayer.setOnErrorListener { mp, what, extra ->
            error("what:$what  extra:$extra")
            when (what) {
                MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                    musicPlayer = (mp as MusicPlayer).getReCreateHelper().reCreate()
                    initMusicPlayer()
                    return@setOnErrorListener true
                }
            }
            false
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return MusicBinder()
    }

    override fun onDestroy() {
        unregisterReceiver(headsetPlugReceiver)
        musicPlayer.release()
        super.onDestroy()
    }

    private var song: BaseSong? = null// 当前播放的歌曲信息

    inner class MusicBinder : Binder() {

        var isLooping: Boolean
            get() = this@MusicService.musicPlayer.isLooping
            set(value) {
                this@MusicService.musicPlayer.isLooping = value
            }

        var isPlaying: Boolean = false
            get() = this@MusicService.musicPlayer.isPlaying
            private set

        var duration: Int = 0
            get() = this@MusicService.musicPlayer.duration
            private set

        var currentPosition: Int
            get() = this@MusicService.musicPlayer.currentPosition
            set(value) {
                if (value > duration) {
                    this@MusicService.musicPlayer.seekTo(duration)
                } else {
                    this@MusicService.musicPlayer.seekTo(value)
                }
            }

        fun getPlaySongInfo(): BaseSong? {
            return this@MusicService.song
        }

        fun getMusicPlayer(): MusicPlayer = this@MusicService.musicPlayer

        fun start() {
            musicPlayer.start()
        }

        fun stop() {
            musicPlayer.stop()
        }

        fun pause() {
            musicPlayer.pause()
        }

        fun play(song: BaseSong?) {
            if (song == null) return
            this@MusicService.song = song

            play(song.path)
        }

        fun autoStart(autoStart: Boolean) {
            musicPlayer.autoStartOnPrepared(autoStart)
        }

        fun play(url: String?) {
            if (url == null) return

            val looping = musicPlayer.isLooping
            musicPlayer.reset()
            musicPlayer.isLooping = looping
            try {
                musicPlayer.setDataSource(url)
                musicPlayer.prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun addOnStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?) {
            musicPlayer.addOnPlayStateChangeListener(listener)
        }

        fun removeOnStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?) {
            musicPlayer.removeOnPlayStateChangeListener(listener)
        }

    }
}