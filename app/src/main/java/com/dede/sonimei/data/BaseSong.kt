package com.dede.sonimei.data

import android.content.Context
import java.io.Serializable

/**
 * Created by hsh on 2018/8/2.
 */
open class BaseSong(open val title: String?,
                    open val path: String?) : Serializable {

    open fun getName(): String {
        return "$title"
    }

    open fun loadPlayLink(load: ContextLoad<String>) {}

    companion object {
        @JvmStatic
        private val serialVersionUID: Long = 7059311150855647095L
    }

    abstract class ContextLoad<I>(val context: Context) {
        abstract fun onSuccess(t: I)
        open fun onFail() {}
    }
}