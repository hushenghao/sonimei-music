package com.dede.sonimei.net

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.*

/**
 * Created by hsh on 2018/5/14.
 */
interface ApiService {

    @GET()
    fun get(@Url url: String, @QueryMap params: Map<String, String>): Observable<String?>

    @FormUrlEncoded
    @POST()
    fun post(@Url url: String, @FieldMap params: Map<String, String>): Observable<String?>

    @GET
    fun download(@Url url: String): Observable<ResponseBody>
}