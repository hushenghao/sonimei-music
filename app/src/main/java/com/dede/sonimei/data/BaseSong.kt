package com.dede.sonimei.data

import com.dede.sonimei.util.extends.applySchedulers
import io.reactivex.Observable
import java.io.Serializable

/**
 * Created by hsh on 2018/8/2.
 */
open class BaseSong(open val title: String?,
                    open var path: String?) : Serializable {

    open fun getName(): String {
        return "$title"
    }

    open fun loadPlayLink(): Observable<String> {
        return Observable.create<String> {
            it.onNext(path ?: "")
        }.applySchedulers()
    }

    companion object {
        @JvmStatic
        private val serialVersionUID: Long = 7059311150855647095L
    }

}