package com.dede.sonimei.module.search

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import com.dede.sonimei.*
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.component.CircularRevealDrawable
import com.dede.sonimei.module.db.DatabaseOpenHelper.Companion.COLUMNS_TEXT
import com.dede.sonimei.module.home.SearchHisAdapter
import com.dede.sonimei.module.home.SourceTypeDialog
import com.dede.sonimei.module.search.netresult.SearchResultResultFragment
import com.dede.sonimei.util.extends.color
import com.dede.sonimei.util.extends.notNull
import com.dede.sonimei.util.extends.to
import kotlinx.android.synthetic.main.fragment_search.*

/**
 * Created by hsh on 2018/8/3.
 * 搜索页面，包括tool bar
 */
class SearchFragment : BaseFragment(), SearchView.OnQueryTextListener {

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView?.clearFocus()
        searchResultFragment.search(query)
        // 保存搜索历史
        hisAdapter?.newSearchHis(query)
        return false// 关闭键盘
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return true
    }

    override fun getLayoutId() = R.layout.fragment_search

    private lateinit var drawable: CircularRevealDrawable
    private lateinit var searchResultFragment: SearchResultResultFragment

    override fun initView(savedInstanceState: Bundle?) {
        activity!!.to<AppCompatActivity>().setSupportActionBar(tool_bar)
        setHasOptionsMenu(true)

        searchResultFragment = childFragmentManager
                .findFragmentById(R.id.search_result_fragment) as SearchResultResultFragment
        val source = searchResultFragment.getTypeSource().first

        drawable = CircularRevealDrawable(color(R.color.colorPrimary))
        app_bar.background = drawable
        app_bar.postDelayed({
            val color = sourceColor(source)
            drawable.play(color)
        }, 500L)
        tv_source_name.text = sourceName(source)

    }

    private var searchView: SearchView? = null
    private var hisAdapter: SearchHisAdapter? = null
    private var autoCompleteText: AutoCompleteTextView? = null

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        searchView = menu!!.findItem(R.id.menu_search).actionView as SearchView?
        val searchType = searchType(SEARCH_NAME)
        tv_search_type.text = searchType
        searchView?.queryHint = searchType
        searchView?.setOnQueryTextListener(this)

        // 内部是继承于AutoCompleteTextView的SearchAutoComplete
        val textView = searchView
                ?.findViewById<AutoCompleteTextView>(R.id.search_src_text)
        this.autoCompleteText = textView
        if (textView != null) {
            textView.setTextColor(Color.WHITE)
            textView.setHintTextColor(Color.argb(180, 255, 255, 255))
            // 因为SearchView&SearchAutoComplete重写了enoughToFilter方法，小于0时返回true直接显示popList
            // 但是会和键盘同时弹起，使pop显示位置不准确，这里不使用返回小于0的数是搜索历史自动显示

            // 监听焦点变化，延迟弹出搜索历史pop
            textView.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    textView.threshold = Int.MAX_VALUE
                } else {
                    // 延时显示，等待键盘弹起
                    v.postDelayed({
                        textView.showDropDown()
                        textView.threshold = -1
                    }, 250)
                }
            }
            textView.setOnItemClickListener { _, _, position, _ ->
                val p = position - SearchHisAdapter.HEADER_COUNT// 去掉header的个数
                if (p < 0) return@setOnItemClickListener
                val cursor = searchView!!.suggestionsAdapter.cursor
                if (p >= cursor.count) return@setOnItemClickListener

                cursor.moveToPosition(p)
                val text = cursor.getString(cursor.getColumnIndex(COLUMNS_TEXT))
                // SearchView&SearchAutoComplete重写replaceText(text)方法为空方法，需要手动设置文本
                searchView!!.setQuery(text, true)
            }
            hisAdapter = SearchHisAdapter(context!!)
            searchView!!.suggestionsAdapter = hisAdapter
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_source_type -> {
                val dialog = SourceTypeDialog(context!!, searchResultFragment.getTypeSource())
                        .callback {
                            val source = it.first
                            tv_source_name.text = sourceName(source)
                            val searchType = searchType(it.second)
                            searchView?.queryHint = searchType
                            tv_search_type.text = searchType
                            val query = searchView?.query?.toString()
                            if (query.notNull()) {
                                searchResultFragment.search(query, it)
                            } else {
                                searchResultFragment.setTypeSource(it)
                            }
                            drawable.play(sourceColor(source))
                        }

                if (searchView?.hasFocus() == true) {
                    val manager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE).to<InputMethodManager>()
                    manager.hideSoftInputFromWindow(autoCompleteText?.windowToken, 0)
                    searchView?.postDelayed({ dialog.show() }, 200)
                } else {
                    dialog.show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        hisAdapter?.cursor?.close()
        super.onDestroy()
    }
}