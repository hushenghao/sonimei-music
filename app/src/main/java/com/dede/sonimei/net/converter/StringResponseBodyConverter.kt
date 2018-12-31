package com.dede.sonimei.net.converter

import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.IOException

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
