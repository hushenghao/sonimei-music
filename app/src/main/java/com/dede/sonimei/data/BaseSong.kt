package com.dede.sonimei.data

import java.io.Serializable

/**
 * Created by hsh on 2018/8/2.
 */
open class BaseSong(open val title: String?,
                    open val path: String?) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 8430639818424013388L
    }

    open fun getName(): String {
        return "$title"
    }
}