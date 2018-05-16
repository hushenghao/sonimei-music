package com.dede.sonimei.net

import android.content.Context
import android.util.ArrayMap
import com.dede.sonimei.BuildConfig
import com.dede.sonimei.net.converter.StringConverterFactory
import io.reactivex.Observable
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by hsh on 2018/5/14.
 */
class HttpUtil private constructor() {

    companion object {
        private lateinit var apiService: ApiService
        private lateinit var retrofit: Retrofit

        fun changeBaseUrl(baseUrl: String) {
            val newBase = HttpUrl.parse(baseUrl) ?: return
            if (retrofit.baseUrl() == newBase) {
                return
            }

            retrofit = retrofit.newBuilder()
                    .baseUrl(newBase)
                    .build()
            apiService = retrofit.create(ApiService::class.java)
        }

        fun init(application: Context) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level =
                    if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
            val httpClient = OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

            retrofit = Retrofit.Builder()
                    .client(httpClient)
                    .baseUrl("http://music.163.com/")
                    .addConverterFactory(StringConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            apiService = retrofit.create(ApiService::class.java)
        }
    }

    class Builder {

        private val params: ArrayMap<String, String> = ArrayMap()
        private var url: String = ""

        fun params(key: String, value: String?): Builder {
            if (value == null)
                return this
            params[key] = value
            return this
        }

        fun url(url: String): Builder {
            this.url = url
            return this
        }

        fun get(): Observable<String?> {
            return apiService.get(this.url, params)
        }

        fun post(): Observable<String?> {
            return apiService.post(this.url, params)
        }
    }

}