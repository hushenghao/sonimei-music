package com.dede.sonimei.module.home.presenter

import com.dede.sonimei.MusicSource
import com.dede.sonimei.NETEASE
import com.dede.sonimei.module.home.ISearchView

/**
 * Created by hsh on 2018/5/15.
 */
object PresenterHelper {

    fun instancePresenter(@MusicSource source: Int, view: ISearchView): BaseSearchPresenter {
        return when (source) {
            NETEASE -> NeteasePresenter(view)
            else -> NeteasePresenter(view)
        }
    }
}