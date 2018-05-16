package com.dede.sonimei.module.home

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.data.search.SearchData

/**
 * Created by hsh on 2018/5/15.
 */
class SearchResultAdapter :
        BaseQuickAdapter<SearchData, BaseViewHolder>(android.R.layout.simple_list_item_1) {

    override fun convert(helper: BaseViewHolder?, item: SearchData?) {
        helper?.setText(android.R.id.text1, item?.name)
    }

}
