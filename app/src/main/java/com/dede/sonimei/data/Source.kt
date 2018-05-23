package com.dede.sonimei.data

import android.support.annotation.ColorInt
import com.dede.sonimei.MusicSource

/**
 * Created by hsh on 2018/5/23.
 */
data class Source(@MusicSource val source: Int,
                  val name: String,
                  val key: String,
                  @ColorInt val color: Int)