package com.dede.sonimei.data.search

/**
 * Created by hsh on 2018/5/15.
 */
data class NeteasData(val result: Result?, val code: Int)

data class Result(val songs: List<Song>?)
data class Song(val id: String?, val name: String?, val ar: List<Singer>?, val al: Album?) {
    fun toSearchData(): SearchData {
        return SearchData(id, name, ar, al)
    }
}
