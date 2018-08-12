package com.dede.sonimei.data

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

/**
 * Created by hsh on 2018/8/2.
 */
open class BaseSong(open val title: String?,
                    open val path: String?) : Serializable, Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString()
    )

    open fun getName(): String {
        return "$title"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(path)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private const val serialVersionUID: Long = 8430639818424013388L

        val CREATOR = object : Parcelable.Creator<BaseSong> {

            override fun createFromParcel(parcel: Parcel): BaseSong {
                return BaseSong(parcel)
            }

            override fun newArray(size: Int): Array<BaseSong?> {
                return arrayOfNulls(size)
            }
        }

    }
}