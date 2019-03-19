package com.dede.sonimei.module.play

import androidx.annotation.IntDef

/**
 * Created by hsh on 2018/9/7 下午2:34
 */

const val STATE_IDLE = 0// 空闲状态
const val STATE_INITIALIZED = 1// 初始化完成状态
const val STATE_PREPARING = 7// 异步准备中状态
const val STATE_PREPARED = 2// 准备完成状态

const val STATE_STARTED = 3// 播放开始状态
const val STATE_PAUSED = 4// 播放暂停状态

const val STATE_STOPED = 5// 播放停止状态

const val STATE_PLAYBACK_COMPLETED = 8// 播放完成状态

const val STATE_END = 6// release()结束状态
const val STATE_ERROR = -1// 错误状态

@Retention(AnnotationRetention.RUNTIME)
@IntDef(STATE_IDLE, STATE_INITIALIZED, STATE_PREPARING, STATE_PREPARED, STATE_STARTED,
        STATE_PAUSED, STATE_STOPED, STATE_PLAYBACK_COMPLETED, STATE_END, STATE_ERROR)
annotation class PlayState