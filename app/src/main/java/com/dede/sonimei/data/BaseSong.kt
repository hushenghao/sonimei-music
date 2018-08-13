package com.dede.sonimei.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

/**
 * Created by hsh on 2018/8/2.
 */
@Parcelize
open class BaseSong(open val title: String?,
                    open val path: String?) : Serializable, Parcelable {

    open fun getName(): String {
        return "$title"
    }

    companion object {
        @JvmStatic
        private val serialVersionUID: Long = 8430639818424013388L
    }
}