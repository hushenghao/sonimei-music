package com.dede.sonimei.data.search

/**
 * Created by hsh on 2018/5/15.
 */
data class SearchSong(val type: String?,
                      val link: String?,// 原链接
                      val songId: String?,
                      val title: String?,
                      val author: String?,
                      val url: String?,// 下载链接
                      val lrc: String?,
                      val pic: String?
) {
    fun getName(): String {
        return "$title - $author"
    }
}