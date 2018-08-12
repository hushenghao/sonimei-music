package com.dede.sonimei.module.setting

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.support.v7.app.AlertDialog
import com.dede.sonimei.*
import com.dede.sonimei.data.Source
import com.dede.sonimei.util.extends.isNull
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.coroutines.experimental.selects.select
import org.jetbrains.anko.*
import java.io.File
import java.io.FileFilter


/**
 * Created by hsh on 2018/5/17.
 */
class Settings : PreferenceFragment(),
        AnkoLogger,
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    companion object {
        const val KEY_CUSTOM_PATH = "custom_path"
        const val KEY_DOWNLOAD_SETTING = "download_setting"
        const val KEY_WIFI_DOWNLOAD = "wifi_download"
        const val KEY_DEFAULT_SEARCH_SOURCE = "default_search_source"
        const val KEY_BUG_REPORT = "bug_report"
    }

    private val selectPathCode = 1
    private val selectSourceCode = 2

    @SuppressLint("InlinedApi")
    override fun onPreferenceClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            KEY_DEFAULT_SEARCH_SOURCE -> {
                startActivityForResult<SelectSourceActivity>(selectSourceCode)
                true
            }
            KEY_CUSTOM_PATH -> {
                if (!isLollipop) {
                    return false
                }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                try {
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_file_dir)),
                            selectPathCode)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
                true
            }
            KEY_BUG_REPORT -> {
                AlertDialog.Builder(context)
                        .setTitle(R.string.emile_theme)
                        .setMessage(R.string.dialog_bug_report)
                        .setNegativeButton(R.string.dont_send) { _, _ -> sendEmail(false) }
                        .setPositiveButton(R.string.do_send) { _, _ -> sendEmail(true) }
                        .create()
                        .show()
                true
            }
            else -> false
        }
    }

    private fun sendEmail(b: Boolean) {
        if (!b) {
            callEmail()
            return
        }
        RxPermissions(activity)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe({
                    if (!it) {
                        callEmail()
                        return@subscribe
                    }
                    var file: File? = null
                    val dir = CrashHandler.instance().crashLogDir()
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.listFiles(FileFilter { f ->
                            return@FileFilter CrashHandler.instance().isLog(f)
                        })
                        if (files?.isNotEmpty() == true) {
                            files.sortByDescending { f -> f.lastModified() }// 按时间倒序
                            file = files[0]// 取最新的
                        }
                    }
                    callEmail(file)
                }, { it.printStackTrace() })
    }

    private fun callEmail(file: File? = null) {
        val email = getString(R.string.email)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "message/rfc882"
        val of = arrayOf(email)
        intent.putExtra(Intent.EXTRA_EMAIL, of)
//        intent.putExtra(Intent.EXTRA_CC, of) // 抄送人
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.emile_theme)) // 主题
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_text)) // 正文
        if (file != null) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.email_chooser)))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEY_WIFI_DOWNLOAD -> {

            }
            KEY_CUSTOM_PATH -> {
                findPreference(KEY_CUSTOM_PATH).summary = defaultSharedPreferences
                        .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
            }
        }
    }

    private val isLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    private lateinit var defaultSearchSource: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference_settings)

        defaultSearchSource = findPreference(KEY_DEFAULT_SEARCH_SOURCE)// 默认搜索来源
        val source = defaultSharedPreferences.getInt(KEY_DEFAULT_SEARCH_SOURCE, normalSource)
        defaultSearchSource.summary = sourceName(source)
        defaultSearchSource.onPreferenceClickListener = this

        val customPath = findPreference(KEY_CUSTOM_PATH)// 自定义下载路径，5.0以下不可用，隐藏设置项
        if (!isLollipop) {
            (findPreference(KEY_DOWNLOAD_SETTING) as PreferenceCategory)
                    .removePreference(customPath)
        } else {
            customPath.onPreferenceClickListener = this
            customPath.summary = defaultSharedPreferences
                    .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
        }
        findPreference(KEY_BUG_REPORT).onPreferenceClickListener = this
    }

    override fun onResume() {
        super.onResume()
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) return
        when (requestCode) {
            selectSourceCode -> {
                val source = data.getSerializableExtra("result") as Source?
                if (source != null) {
                    defaultSearchSource.summary = source.name
                    defaultSharedPreferences.edit()
                            .putInt(KEY_DEFAULT_SEARCH_SOURCE, source.source)
                            .apply()
                }
            }
            selectPathCode -> {
                val uri = data.data
                info(uri.toString())
                val path = SettingHelper.documentUri2Path(activity, uri)
                info(path)
                if (path.isNull() || File(path).canRead()) {
                    toast("路径不可用")
                    return
                }
                defaultSharedPreferences.edit()
                        .putString(KEY_CUSTOM_PATH, path)
                        .apply()
                // 这时候activity不可见，所以onSharedPreferenceChanged不会回调
                findPreference(KEY_CUSTOM_PATH).summary = defaultSharedPreferences
                        .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}