package com.dede.sonimei.module.play

interface ILoadPlayList {

    /**
     * 读取播放列表完成的回调
     */
    interface OnLoadPlayListListener {
        fun onLoadFinish()
    }

    /**
     * 加载已经序列化播放列表，上一次的播放列表
     */
    fun setLoadPlayListListener(loadPlayListListener: OnLoadPlayListListener?)
}