package com.dede.sonimei.data.search

import android.os.Parcel
import android.os.Parcelable
import com.dede.sonimei.data.BaseData
import com.dede.sonimei.net.HttpUtil
import com.dede.sonimei.util.NetEaseWeb
import com.dede.sonimei.util.extends.applySchedulers
import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import org.json.JSONArray

/**
 * 网易云搜索接口返回数据结构
 */
data class NetEaseWebResult(@SerializedName("id") val songId: String?,
                            val name: String?,
                            val ar: List<Ar>?,
                            val al: Al?) {

    class Ar(val name: String?)
    class Al(@SerializedName("picUrl") val pic: String?)

    fun map(): NetEaseSong {
        val ar = ar?.map { it.name }?.reduce { acc, n -> "$acc/$n" }
        return NetEaseSong(songId, name, ar, al?.pic)
    }
}

/**
 * 转换后的结构
 */
data class NetEaseSong(override val songId: String?,
                       override val title: String?,
                       override val author: String?,
                       override val pic: String?)
    : SearchSong("netease_web", null, songId, title, author, null, null, pic) {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()
    )

    override fun loadPlayLink(): Observable<String> {
        if (songId != null) {
            // 播放链接失效很快，每次都重新取
            return HttpUtil.Builder()
                    .url(NetEaseWeb.netEaseWebRequestUrl(songId))
                    .post()
                    .applySchedulers()
                    .map { BaseData(it) }
                    .filter { it.code == 200 }
                    .map { it.data }
                    .map { JSONArray(it).getJSONObject(0).optString("url") }
                    .doOnNext { path = it }
        }
        return super.loadPlayLink()
    }

    override fun canPlay(): Boolean {
        return songId != null
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<NetEaseSong> {
            override fun createFromParcel(parcel: Parcel): NetEaseSong {
                return NetEaseSong(parcel)
            }

            override fun newArray(size: Int): Array<NetEaseSong?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(songId)
        parcel.writeString(title)
        parcel.writeString(author)
        parcel.writeString(pic)
    }

    override fun describeContents(): Int {
        return 0
    }
}