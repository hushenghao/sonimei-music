package com.dede.sonimei.module.setting

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import com.dede.sonimei.NETEASE
import com.dede.sonimei.R
import com.dede.sonimei.data.Source
import com.dede.sonimei.defaultDownloadPath
import com.dede.sonimei.sourceName
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.info
import org.jetbrains.anko.startActivityForResult

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
                    startActivityForResult(Intent.createChooser(intent, "选择文件路径"),
                            selectPathCode)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
                true
            }
            else -> false
        }
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
        val source = defaultSharedPreferences.getInt(KEY_DEFAULT_SEARCH_SOURCE, NETEASE)
        defaultSearchSource.summary = sourceName(source)
        defaultSearchSource.onPreferenceClickListener = this

        val customPath = findPreference(KEY_CUSTOM_PATH)// 自定义下载路径，5.0一下不可用，隐藏设置项
        if (!isLollipop) {
            (findPreference(KEY_DOWNLOAD_SETTING) as PreferenceCategory)
                    .removePreference(customPath)
        } else {
            customPath.onPreferenceClickListener = this
            customPath.summary = defaultSharedPreferences
                    .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
        }
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