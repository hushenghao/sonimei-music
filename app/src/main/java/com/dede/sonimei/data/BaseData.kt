package com.dede.sonimei.data

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by hsh on 2018/5/16.
 */
class BaseData(json: String?) {
    var code: Int = -1
    var data: String = ""
    var error: String = ""

    init {
        try {
            val jsonObject = JSONObject(json)
            code = jsonObject.optInt("code", -1)
            error = jsonObject.optString("error", "")
            data = jsonObject.optString("data", "")
        } catch (e: JSONException) {
        }
    }

    fun trueStatus() = code == 200
}