package cn.yiiguxing.plugin.translate.util

import java.util.*

/**
 * ArrayEnumeration
 *
 * Created by Yii.Guxing on 2017-11-12 0012.
 */
class ArrayEnumeration<T>(private val array: Array<T>) : Enumeration<T> {
    private var current: Int = 0

    override fun hasMoreElements(): Boolean {
        return current < array.size
    }

    override fun nextElement(): T {
        return if (current < array.size) {
            array[current++]
        } else {
            throw NoSuchElementException()
        }
    }
}