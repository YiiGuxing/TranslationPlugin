/**
 * Lists
 * <p>
 * Created by Yii.Guxing on 2017-09-16 0016.
 */
package cn.yiiguxing.plugin.translate.util

import java.util.*


fun <T> MutableList<T>.trimToSize(maxSize: Int): Boolean {
    var size = this.size
    val trim = size > 0 && size > maxSize
    when {
        trim && maxSize <= 0 -> clear()
        trim -> while (size > maxSize) removeAt(--size)
    }

    return trim
}

fun <T> List<T>.toJVMReadOnlyList(): List<T> = Collections.unmodifiableList(this)

inline fun <reified T> Collection<T>.toEnumeration(): Enumeration<T> = ArrayEnumeration(toTypedArray())