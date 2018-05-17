package com.dede.sonimei.net

import io.reactivex.Observable
import retrofit2.http.*

/**
 * Created by hsh on 2018/5/14.
 */
interface ApiService {

    @GET()
    fun get(@Url url: String,
            @HeaderMap headers: Map<String, String>,
            @QueryMap params: Map<String, String>): Observable<String?>

    @FormUrlEncoded
    @POST()
    fun post(@Url url: String,
             @HeaderMap headers: Map<String, String>,
             @FieldMap params: Map<String, String>): Observable<String?>

}