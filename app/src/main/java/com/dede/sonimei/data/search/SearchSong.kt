package com.dede.sonimei.data.search

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
    override fun getName(): String {
        return "$title - $author"
    }
}