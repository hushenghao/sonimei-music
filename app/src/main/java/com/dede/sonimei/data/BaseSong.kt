package com.dede.sonimei.data

/**
 * Created by hsh on 2018/8/2.
 */
open class BaseSong(open val title: String?,
                    open val path: String?) {
    open fun getName(): String {
        return "$title"
    }
}