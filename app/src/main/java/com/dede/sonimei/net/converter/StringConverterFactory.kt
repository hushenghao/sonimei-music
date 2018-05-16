package com.dede.sonimei.net.converter

import java.lang.reflect.Type

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * Created by hsh on 2016/9/6.
 */
class StringConverterFactory private constructor() : Converter.Factory() {

    override fun responseBodyConverter(type: Type?, annotations: Array<Annotation>?,
                                       retrofit: Retrofit?): Converter<ResponseBody, *>? {
        return StringResponseBodyConverter()
    }

    override fun requestBodyConverter(type: Type?, parameterAnnotations: Array<Annotation>?,
                                      methodAnnotations: Array<Annotation>?,
                                      retrofit: Retrofit?): Converter<*, RequestBody>? {
        return StringRequestBodyConverter()
    }

    companion object {

        fun create(): StringConverterFactory {
            return StringConverterFactory()
        }
    }
}