package com.dede.sonimei.util.extends

import com.trello.rxlifecycle2.LifecycleProvider
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by hsh on 2018/5/14.
 */
fun <T> Observable<T>.applyFragmentLifecycle(provider: LifecycleProvider<FragmentEvent>): Observable<T> {
    return this.applySchedulers()
            .bindUntilEvent(provider, FragmentEvent.DESTROY_VIEW)
}

fun <T> Observable<T>.applyActivityLifecycle(provider: LifecycleProvider<ActivityEvent>): Observable<T> {
    return this.applySchedulers()
            .bindUntilEvent(provider, ActivityEvent.DESTROY)
}


fun <T> Observable<T>.applySchedulers(): Observable<T> {
    return this.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}
