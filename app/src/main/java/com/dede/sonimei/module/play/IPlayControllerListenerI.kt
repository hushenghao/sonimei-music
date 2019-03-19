package com.dede.sonimei.module.play

import android.os.Build
import androidx.annotation.RequiresApi
import com.dede.sonimei.data.BaseSong

/**
 * Created by hsh on 2018/8/6.
 * 播放控制接口
 */
interface IPlayControllerListenerI : IReviseOnPlayStateChangeListener {

    /**
     * 获取当前播放列表
     */
    fun getPlayList(): List<BaseSong>

    /**
     * 从当前播放列表移除当前位置的歌曲。
     * 如果是当前播放的歌曲，就自动播放下一首；如果移除后播放列表为空，就停止播放
     */
    fun removeAt(index: Int)

    /**
     * 移除当前播放歌曲对象
     */
    fun remove(song: BaseSong?)

    /**
     * 添加将新的歌曲到播放列表，索引不传就添加到最后一个
     */
    fun add(song: BaseSong?, index: Int = -1)

    /**
     * 清除播放列表
     */
    fun clear()

    /**
     * 播放当前播放列表。
     * 如果当前播放列表不存在此歌曲，就添加到播放列表
     */
    fun play(song: BaseSong?)

    /**
     * 清空当前播放列表，播放新的列表，从index处开始播放
     */
    fun plays(playList: List<BaseSong>?, index: Int = 0)

    /**
     * 开始播放
     */
    fun start()

    /**
     * 暂停播放
     */
    fun pause()

    /**
     * 下一首
     */
    fun next()

    /**
     * 上一首
     */
    fun last()

    /**
     * 播放速度get set方法，传null就是get方法
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun sheep(sheep: Float = -1f): Float

    /**
     * 修改列表播放模式，循序播放，单曲循环，随机播放
     */
    fun updatePlayMode(@PlayMode mode: Int)

    /**
     * 获取当前播放模式
     */
    @PlayMode
    fun getPlayMode(): Int

    /**
     * 获取当前播放的歌曲信息
     */
    fun getPlayInfo(): BaseSong?

    /**
     * 获取当前播放的歌曲在播放列表的索引
     */
    fun getPlayIndex(): Int

}