package com.dede.sonimei.util.extends

/**
 * Created by hsh on 2018/6/27.
 */

/**
 * 强转
 */
fun <T> Any.to(): T {
    return this as T
}