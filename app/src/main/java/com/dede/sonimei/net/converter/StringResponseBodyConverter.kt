package com.dede.sonimei.net.converter

import java.io.IOException

import okhttp3.ResponseBody
import retrofit2.Converter

/**
 * Created by hsh on 2016/9/6.
 */
class StringResponseBodyConverter : Converter<ResponseBody, String> {
    @Throws(IOException::class)
    override fun convert(value: ResponseBody): String {
        try {
            return value.string()
        } finally {
            value.close()
        }
    }
}
