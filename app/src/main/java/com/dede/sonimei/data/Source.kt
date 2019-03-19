package com.dede.sonimei.data

import androidx.annotation.ColorInt
import com.dede.sonimei.MusicSource
import java.io.Serializable

/**
 * Created by hsh on 2018/5/23.
 */
data class Source(@MusicSource val source: Int,
                  val name: String,
                  val key: String,
                  @ColorInt val color: Int) : Serializable