package com.dede.sonimei.module.play

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.player.MusicPlayer

/**
 * Created by hsh on 2018/8/6.
 */
class MusicBinder(val service: MusicService) : Binder(), IPlayControllerListenerI, ILoadPlayList {

    override fun setLoadPlayListListener(loadPlayListListener: ILoadPlayList.OnLoadPlayListListener?) {
        service.setLoadPlayListListener(loadPlayListListener)
    }

    override fun getPlayIndex(): Int {
        return service.getPlayIndex()
    }

    override fun getPlayList(): List<BaseSong> {
        return service.getPlayList()
    }

    override fun removeAt(index: Int) {
        service.removeAt(index)
    }

    override fun remove(song: BaseSong?) {
        service.remove(song)
    }

    override fun add(song: BaseSong?, index: Int) {
        service.add(song, index)
    }

    override fun clear() {
        service.clear()
    }

    private val player = service.getMusicPlayer()

    var isLooping: Boolean
        get() = this.player.isLooping
        set(value) {
            this.player.isLooping = value
        }

    var isPlaying: Boolean = false
        get() = this.player.isPlaying
        private set

    var duration: Int = 0
        get() = this.player.duration
        private set

    var currentPosition: Int
        get() = this.player.currentPosition
        set(value) {
            if (value > duration) {
                this.player.seekTo(duration)
            } else {
                this.player.seekTo(value)
            }
        }

    override fun updatePlayMode(@PlayMode mode: Int) {
        service.updatePlayMode(mode)
    }

    @PlayMode
    override fun getPlayMode(): Int {
        return service.getPlayMode()
    }

    override fun getPlayInfo(): BaseSong? {
        return service.getPlayInfo()
    }

    fun getPlayer(): MusicPlayer = this.player

    override fun start() {
        service.start()
    }

    override fun pause() {
        service.pause()
    }

    override fun play(song: BaseSong?) {
        service.play(song)
    }

    override fun plays(playList: List<BaseSong>?, index: Int) {
        service.plays(playList, index)
    }

    override fun next() {
        service.next()
    }

    override fun last() {
        service.last()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun sheep(sheep: Float): Float {
        return service.sheep(sheep)
    }

    override fun addOnPlayStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?) {
        service.addOnPlayStateChangeListener(listener)
    }

    override fun removeOnPlayStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?) {
        service.removeOnPlayStateChangeListener(listener)
    }

}