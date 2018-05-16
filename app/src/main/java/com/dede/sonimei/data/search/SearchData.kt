package com.dede.sonimei.data.search

/**
 * Created by hsh on 2018/5/15.
 */
data class SearchData(
        val id: String?,
        val name: String?,
        val singer: List<Singer>?,
        val album: Album?) {
    fun singerStr(): String {
        val buffer = StringBuffer()
        singer?.forEach {
            buffer.append(",")
                    .append(it.name)
        }
        if (buffer.isNotEmpty()) {
            buffer.deleteCharAt(0)
        }
        return buffer.toString()
    }
}

data class Singer(val id: String?, val name: String?)
data class Album(val id: String?, val name: String?, val picUrl: String?)