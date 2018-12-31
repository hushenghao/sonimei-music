package com.dede.sonimei.module.selector

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.util.extends.gone
import com.dede.sonimei.util.extends.notNull
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_file_selector.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileFilter

const val FILE_SELECT_PATH = 0
const val FILE_SELECT_FILE = 1
const val EXTRA_SELECT_TYPE = "select_type"
const val EXTRA_SELECTOR_TITLE = "selector_title"
const val EXTRA_INIT_PATH = "init_path"
const val RESULT_FILE_PATH = "result_path"

/**
 * 自定义文件路径选择器
 */
class FileSelectorActivity : BaseActivity() {

    private val rootDir by lazy { Environment.getExternalStorageDirectory() }

    override fun getLayoutId() = R.layout.activity_file_selector

    private lateinit var fileFilter: FileFilter
    private lateinit var thisPath: File

    @SuppressLint("CheckResult")
    override fun initView(savedInstanceState: Bundle?) {
        val fileAdapter = FileAdapter()
        recycler_view.adapter = fileAdapter

        if (savedInstanceState != null) {
            intent.putExtras(savedInstanceState)
        }

        val title = intent.getStringExtra(EXTRA_SELECTOR_TITLE)
        if (title != null) {
            setTitle(title)
        }
        val selectType = intent.getIntExtra(EXTRA_SELECT_TYPE, FILE_SELECT_PATH)
        fileFilter = when (selectType) {
            FILE_SELECT_PATH -> {
                PathFilter()
            }
            else -> {
                fab_done.gone()
                CFileFilter()
            }
        }
        thisPath = rootDir

        RxPermissions(this)
                .request(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({
                    if (!it) {
                        toast(R.string.permission_sd_error)
                        return@subscribe
                    }
                    val pathStr = intent.getStringExtra(EXTRA_INIT_PATH)
                    if (pathStr.notNull()) {
                        val path = File(pathStr)
                        if (!path.exists()) {
                            path.mkdirs()
                        }
                        if (path.isDirectory && inRootDir(path)) {
                            thisPath = path
                        }
                    }
                    setNewData(thisPath)
                }) { it.printStackTrace() }

        fab_done.setOnClickListener { selectFile(thisPath) }
        tv_full_path.setOnLongClickListener {
            val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.primaryClip = ClipData.newPlainText(null, tv_full_path.text)
            toast(R.string.copy_succ)
            return@setOnLongClickListener true
        }
        val headerView = LayoutInflater.from(this).inflate(R.layout.item_file_selector, recycler_view, false)
        headerView.findViewById<TextView>(R.id.tv_path_name).setText(R.string.return_parent_dir)
        headerView.setOnClickListener {
            val parentFile = thisPath.parentFile
            if (inRootDir(parentFile)) {
                setNewData(parentFile)
            } else {
                toast(R.string.toast_root_dir)
            }
        }
        fileAdapter.addHeaderView(headerView)
        fileAdapter.setOnItemClickListener { adapter, _, position ->
            val list = adapter.data as List<File>
            val file = list[position]
            if (file.isFile) {
                selectFile(file)
            } else if (file.isDirectory) {
                setNewData(file)
            }
        }
    }

    private fun setNewData(path: File) {
        thisPath = path
        tv_full_path.isSelected = true
        tv_full_path.text = path.absolutePath
        val fileAdapter = recycler_view.adapter as FileAdapter
        fileAdapter.setNewData(getFileList(path))
    }

    private fun inRootDir(file: File?): Boolean {
        if (rootDir == file) {
            return true
        }
        var path: File? = file
        while (path != null) {
            val parentFile = path.parentFile
            if (rootDir != parentFile) {
                path = parentFile
            } else {
                return true
            }
        }
        return false
    }

    private fun getFileList(dir: File): List<File> {
        val listFiles = dir.listFiles(fileFilter) ?: return emptyList()
        val list = listFiles.asList().toMutableList()
        list.sortWith(Comparator<File> { o1, o2 -> o1?.name?.compareTo(o2?.name ?: "") ?: 0 })
        return list
    }

    private fun selectFile(file: File) {
        val intent = Intent()
                .putExtra(RESULT_FILE_PATH, file.absolutePath)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(EXTRA_SELECT_TYPE, intent.getIntExtra(EXTRA_SELECT_TYPE, FILE_SELECT_PATH))
        outState?.putString(EXTRA_INIT_PATH, thisPath.absolutePath)
        outState?.putString(EXTRA_SELECTOR_TITLE, intent.getStringExtra(EXTRA_SELECTOR_TITLE))
    }

    private inner class FileAdapter : BaseQuickAdapter<File, BaseViewHolder>(R.layout.item_file_selector) {
        override fun convert(helper: BaseViewHolder?, item: File?) {
            helper?.setText(R.id.tv_path_name, item?.name)
                    ?.setVisible(R.id.iv_dir_icon, item?.isDirectory ?: false)
        }
    }

    private class CFileFilter : FileFilter {
        override fun accept(pathname: File?): Boolean {
            return pathname != null && pathname.canExecute()
        }
    }

    private class PathFilter : FileFilter {
        override fun accept(pathname: File?): Boolean {
            return pathname != null && pathname.canExecute() && pathname.isDirectory
        }
    }
}