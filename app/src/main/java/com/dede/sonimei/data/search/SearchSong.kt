package com.dede.sonimei.data.search

import android.os.Parcel
import android.os.Parcelable
import com.dede.sonimei.data.BaseSong
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by hsh on 2018/5/15.
 */
data class SearchSong(@Expose val type: String?,
                      @Expose val link: String?,// 原链接
                      @Expose val songId: String?,
                      @Expose override val title: String?,
                      @Expose val author: String?,
                      @Expose @SerializedName("url") override val path: String?,// 下载链接
                      @Expose val lrc: String?,
                      @Expose val pic: String?
) : BaseSong(title, path) {

    constructor(parcel: Parcel) : this(parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()
    )

    override fun getName(): String {
        return "$title - $author"
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<SearchSong> {
            override fun createFromParcel(parcel: Parcel): SearchSong {
                return SearchSong(parcel)
            }

            override fun newArray(size: Int): Array<SearchSong?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeString(link)
        parcel.writeString(songId)
        parcel.writeString(title)
        parcel.writeString(author)
        parcel.writeString(path)
        parcel.writeString(lrc)
        parcel.writeString(pic)
    }

    override fun describeContents(): Int {
        return 0
    }

}