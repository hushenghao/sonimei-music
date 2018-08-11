package com.dede.sonimei.module.play

import com.dede.sonimei.player.MusicPlayer

/**
 * 修改播放状态监听接口
 */
interface IReviseOnPlayStateChangeListener {

    /**
     * 添加播发状态改变监听
     */
    fun addOnPlayStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?)

    /**
     * 移除播放状态改变监听。
     * 如果传null，就清空所有监听
     */
    fun removeOnPlayStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?)
}