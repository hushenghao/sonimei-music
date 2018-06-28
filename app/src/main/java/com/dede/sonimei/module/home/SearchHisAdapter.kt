package com.dede.sonimei.module.home

import android.content.Context
import android.database.Cursor
import android.support.annotation.WorkerThread
import android.support.v4.content.ContextCompat
import android.support.v4.widget.CursorAdapter
import android.support.v4.widget.SimpleCursorAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dede.sonimei.R
import com.dede.sonimei.module.db.DatabaseOpenHelper
import com.dede.sonimei.module.db.db
import com.dede.sonimei.util.extends.isNull
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.replace
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by hsh on 2018/6/27.
 * 搜索历史adapter
 */
class SearchHisAdapter(context: Context)
    : SimpleCursorAdapter(context, R.layout.item_search_his, null,
        arrayOf(DatabaseOpenHelper.COLUMNS_TEXT), intArrayOf(R.id.tv_query),
        CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {

    companion object {
        const val ITEM_NORMAL = 0// 普通item
        const val ITEM_HEADER = 1// 清空搜索历史header

        const val ITEM_TYPE_COUNT = 2// item类型数量

        const val HEADER_COUNT = 1// header个数
    }

    init {
        // 查询数据库
        doAsync {
            val newCursor = query(null)
            uiThread {
                changeCursor(newCursor)
            }
        }

        setFilterQueryProvider {
            // 文字改变时重新查询数据库，模糊匹配，on WorkThread
            query(it?.toString())
        }
    }

    override fun getCount(): Int {
        val count = super.getCount()
        if (count == 0) {
            return count
        }
        return count + HEADER_COUNT
    }

    override fun getItem(position: Int): Any? {
        return when (getItemViewType(position)) {
            ITEM_NORMAL -> super.getItem(position - HEADER_COUNT)
            else -> null
        }
    }

    override fun getViewTypeCount(): Int {
        return ITEM_TYPE_COUNT
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return ITEM_HEADER
        }
        return ITEM_NORMAL
    }

    /**
     * 查询数据库
     */
    @WorkerThread
    private fun query(text: String?): Cursor? {
        return if (text.isNull()) {
            mContext.db.query(DatabaseOpenHelper.TABLE_SEARCH_HIS, null,
                    null, null,
                    null, null, "${DatabaseOpenHelper.COLUMNS_TIMESTAMP} DESC")
        } else {
            mContext.db.query(DatabaseOpenHelper.TABLE_SEARCH_HIS, null,
                    "${DatabaseOpenHelper.COLUMNS_TEXT} LIKE ?", arrayOf(text),
                    null, null, "${DatabaseOpenHelper.COLUMNS_TIMESTAMP} DESC")
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        when (getItemViewType(position)) {
            ITEM_NORMAL -> {
                val p = position - HEADER_COUNT
                val view = super.getView(p, convertView, parent)
                view.findViewById<View>(R.id.iv_delete)?.setOnClickListener {
                    doAsync {
                        if (p >= cursor.count) return@doAsync
                        cursor.moveToPosition(p)
                        val id = cursor.getInt(cursor.getColumnIndex(DatabaseOpenHelper.COLUMNS_ID))
                        if (id <= 0) return@doAsync
                        val dId = mContext.db.delete(DatabaseOpenHelper.TABLE_SEARCH_HIS,
                                "${DatabaseOpenHelper.COLUMNS_ID} = ?", arrayOf(id.toString()))
                        if (dId <= 0) return@doAsync
                        val newCursor = query(null)
                        uiThread {
                            changeCursor(newCursor)
                        }
                    }
                }
                return view
            }
            ITEM_HEADER -> {
                val textView = convertView as? TextView ?: TextView(mContext)
                textView.text = "清空搜索历史"
                textView.setTextColor(ContextCompat.getColor(mContext, R.color.text2))
                textView.textSize = 14f
                textView.setPadding(mContext.dip(15), mContext.dip(15), 0, mContext.dip(5))
                textView.setOnClickListener {
                    doAsync {
                        mContext.db.delete(DatabaseOpenHelper.TABLE_SEARCH_HIS, null, null)
                        val cursor = query(null)
                        uiThread {
                            changeCursor(cursor)
                        }
                    }
                }
                return textView
            }
        }
        return convertView
    }

    /**
     * 修改或插入新的数据
     */
    fun newSearchHis(query: String?) {
        if (query.isNull()) return
        if (cursor == null) return
        doAsync {
            var id = -1
            var move2First = cursor.moveToFirst()// 移动到第一个，因为Adapter内部也操作了Cursor的位置
            while (move2First || cursor.moveToNext()) {
                if (move2First) move2First = false
                val text = cursor.getString(cursor.getColumnIndex(DatabaseOpenHelper.COLUMNS_TEXT))
                if (text == query) {
                    id = cursor.getInt(cursor.getColumnIndex(DatabaseOpenHelper.COLUMNS_ID))
                    break
                }
            }
            // 修改或插入新的数据
            if (id != -1) {
                mContext.db.replace(DatabaseOpenHelper.TABLE_SEARCH_HIS,
                        DatabaseOpenHelper.COLUMNS_ID to id,// 通过id(主键)确认唯一
                        DatabaseOpenHelper.COLUMNS_TEXT to query,
                        DatabaseOpenHelper.COLUMNS_TIMESTAMP to System.currentTimeMillis() / 1000)
            } else {
                mContext.db.insert(DatabaseOpenHelper.TABLE_SEARCH_HIS, DatabaseOpenHelper.COLUMNS_TEXT to query)
            }
        }
    }
}