package com.dede.sonimei.data.changelog

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ChangeLog(@SerializedName("version_name") val versionName: String,
                     @SerializedName("version_code") val versionCode: Int,
                     @SerializedName("change_log") val changeLog: List<String>) : Parcelable {

    fun getTitle(): String {
        if (versionCode <= 0) return versionName
        return "$versionName($versionCode)"
    }
}
