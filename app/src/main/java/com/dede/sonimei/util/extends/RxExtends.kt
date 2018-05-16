package com.dede.sonimei.util.extends

import com.dede.sonimei.module.home.ISearchView
import com.trello.rxlifecycle2.LifecycleProvider
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.lang.reflect.ParameterizedType

/**
 * Created by hsh on 2018/5/14.
 */
fun <T> Observable<T>.applyLifecycle(provider: LifecycleProvider<*>): Observable<T> {
    val genericSuperclass = provider.javaClass.genericSuperclass
    if (genericSuperclass is ParameterizedType) {
        val type = genericSuperclass.actualTypeArguments[0]
        when (type) {
            is ActivityEvent -> {
                this.bindUntilEvent(provider as LifecycleProvider<ActivityEvent>, ActivityEvent.DESTROY)
            }
            is FragmentEvent -> {
                this.bindUntilEvent(provider as LifecycleProvider<FragmentEvent>, FragmentEvent.DESTROY_VIEW)
            }
        }
    }
    return this.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}
