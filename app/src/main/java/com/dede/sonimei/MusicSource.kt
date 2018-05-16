package com.dede.sonimei

import android.support.annotation.IntDef
import android.util.SparseArray

/**
 * Created by hsh on 2018/5/15.
 */
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

@Retention(AnnotationRetention.RUNTIME)
@IntDef(NETEASE, _1TING, BAIDU, KUGOU, KUWO, QQ, XIAMI, _5SINGYC, _5SINGFC, MIGU, LIZHI, QINGTING, XIMALAYA, KG)
annotation class MusicSource

private val sourceArray by lazy {
    val array = SparseArray<String>()
    array.put(NETEASE, "网易")
    array.put(QQ, "QQ")
    array.put(KUGOU, "酷狗")
    array.put(KUWO, "酷我")
    array.put(XIAMI, "虾米")
    array.put(BAIDU, "百度")
    array.put(_1TING, "一听")
    array.put(MIGU, "咪咕")
    array.put(LIZHI, "荔枝")
    array.put(QINGTING, "蜻蜓")
    array.put(XIMALAYA, "喜马拉雅")
    array.put(KG, "全民K歌")
    array.put(_5SINGYC, "5sing原创")
    array.put(_5SINGFC, "5sing翻唱")
    return@lazy array
}

val sourceKey: ArrayList<Int> by lazy {
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
//            KG,// 暂时搜索接口不可用
            _5SINGYC,
            _5SINGFC
    )
}

fun sourceName(@MusicSource key: Int): String = sourceArray[key]

