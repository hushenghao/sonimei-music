package com.dede.sonimei

import android.os.Environment
import android.support.annotation.IntDef
import android.util.SparseArray
import com.dede.sonimei.data.Source
import com.dede.sonimei.data.opensource.OpenSource
import java.io.File

/**
 * Created by hsh on 2018/5/15.
 */

/** 一些链接 */
const val APE_LINK = "http://music.sonimei.cn/ape/"
const val GITHUB_LINK = "https://github.com/hushenghao/music/"
const val WEB_LINK = "http://music.sonimei.cn/"

/** 音乐来源 */
const val NETEASE: Int = 1// 网易云
const val _1TING: Int = 2// 一听
const val BAIDU: Int = 3
const val KUGOU: Int = 4
const val KUWO: Int = 5
const val QQ: Int = 6// qq
const val XIAMI: Int = 7// 虾米
const val _5SINGYC: Int = 8// 51原唱
const val _5SINGFC: Int = 9// 51翻唱
const val MIGU: Int = 10// 咪咕
const val LIZHI: Int = 11// 荔枝
const val QINGTING: Int = 12// 蜻蜓
const val XIMALAYA: Int = 13// 喜马拉雅
const val KG: Int = 14// 全民k歌

/**
 * 音乐来源
 */
@Retention(AnnotationRetention.RUNTIME)
@IntDef(NETEASE, _1TING, BAIDU, KUGOU, KUWO, QQ, XIAMI, _5SINGYC, _5SINGFC, MIGU, LIZHI, QINGTING, XIMALAYA, KG)
annotation class MusicSource

private val sourceMap by lazy {
    val array = SparseArray<Source>()
    array.put(NETEASE, Source(NETEASE, "网易云", "netease", 0xffD90C0D.toInt()))
    array.put(QQ, Source(QQ, "QQ", "qq", 0xff30C27C.toInt()))
    array.put(KUGOU, Source(KUGOU, "酷狗", "kugou", 0xff3585FB.toInt()))
    array.put(KUWO, Source(KUWO, "酷我", "kuwo", 0xffFCAB3A.toInt()))
    array.put(XIAMI, Source(XIAMI, "虾米", "xiami", 0xffF77D1D.toInt()))
    array.put(BAIDU, Source(BAIDU, "百度", "baidu", 0xff3484FF.toInt()))
    array.put(_1TING, Source(_1TING, "一听", "1ting", 0xff03A7FF.toInt()))
    array.put(MIGU, Source(MIGU, "咪咕", "migu", 0xffED0080.toInt()))
    array.put(LIZHI, Source(LIZHI, "荔枝", "lizhi", 0xffD60150.toInt()))
    array.put(QINGTING, Source(QINGTING, "蜻蜓", "qingting", 0xffF63C3D.toInt()))
    array.put(XIMALAYA, Source(XIMALAYA, "喜马拉雅", "ximalaya", 0xffE02D16.toInt()))
    array.put(KG, Source(KG, "全民K歌", "kg", 0xffF05449.toInt()))
    array.put(_5SINGYC, Source(_5SINGYC, "5sing原创 ", "5singyc ", 0xff202224.toInt()))
    array.put(_5SINGFC, Source(_5SINGFC, "5sing翻唱 ", "5singfc ", 0xff202224.toInt()))
    return@lazy array
}

val normalSource by lazy { QQ }// 默认来源
val normalType by lazy { SEARCH_NAME }// 默认搜索类型

val sourceArray: ArrayList<Int> by lazy {
    arrayListOf(
            NETEASE,
            QQ,
            KUGOU,
            KUWO,
            XIAMI,
            BAIDU,
            _1TING,
            MIGU,
            LIZHI,
            QINGTING,
            XIMALAYA,
            KG,
            _5SINGYC,
            _5SINGFC
    )
}

val sourceList: ArrayList<Source> by lazy {
    ArrayList(sourceArray.map { sourceMap[it] })
}

fun sourceName(@MusicSource key: Int): String = sourceMap[key].name
fun sourceKey(@MusicSource key: Int): String = sourceMap[key].key
fun sourceColor(@MusicSource key: Int): Int = sourceMap[key].color

const val SEARCH_NAME = "name"
const val SEARCH_ID = "id"
const val SEARCH_URL = "path"

fun searchType(searchType: String): String {
    return when (searchType) {
        SEARCH_NAME -> "音乐名称"
        SEARCH_ID -> "音乐ID"
        SEARCH_URL -> "音乐地址"
        else -> "音乐名称"
    }
}

/** 默认下载路径 */
val defaultDownloadPath: File by lazy { File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "sonimei") }

const val defaultSheepIndex = 2
const val defaultSheep = 1f

/**
 * 播放速度集合
 */
val sheepList = arrayListOf(
        0.5f,
        1f,
        1.5f,
        2f
)