package com.dede.sonimei.data

import android.os.Parcel
import android.os.Parcelable
import com.dede.sonimei.util.extends.applySchedulers
import io.reactivex.Observable
import java.io.Serializable

/**
 * Created by hsh on 2018/8/2.
 */
open class BaseSong(open val title: String?,
                    open var path: String?) : Serializable, Parcelable {

    constructor(parcel: Parcel) : this(parcel.readString(),
            parcel.readString()
    )

    open fun getName(): String {
        return "$title"
    }

    open fun loadPlayLink(): Observable<String> {
        return Observable.create<String> {
            it.onNext(path ?: "")
        }
    }

    open fun loadLrc(): Observable<String> {
        return Observable.create<String> {
            it.onNext("")
        }
    }

    companion object {
        @JvmStatic
        private val serialVersionUID: Long = 7059311150855647095L

        @JvmField
        val CREATOR = object : Parcelable.Creator<BaseSong> {
            override fun createFromParcel(parcel: Parcel): BaseSong {
                return BaseSong(parcel)
            }

            override fun newArray(size: Int): Array<BaseSong?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(path)
    }

    override fun describeContents(): Int {
        return 0
    }

}