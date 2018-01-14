/*
 * Logs
 * 
 * Created by Yii.Guxing on 2017/10/30
 */
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.diagnostic.Logger


inline fun Logger.d(lazyMessage: () -> String) {
    lazyMessage().let {
        println("DEBUG - $it")
        debug(it)
    }
}

inline fun Logger.i(lazyMessage: () -> String) {
    lazyMessage().let {
        println("INFO - $it")
        info(it)
    }
}

fun Logger.w(tr: Throwable) {
    w(tr) { tr.message.toString() }
}

inline fun Logger.w(tr: Throwable? = null, lazyMessage: () -> String) {
    lazyMessage().let {
        println("WARN - $it")
        tr?.printStackTrace()
        warn(it, tr)
    }
}

inline fun Logger.e(tr: Throwable? = null, vararg details: String, lazyMessage: () -> String) {
    lazyMessage().let {
        println("ERROR - $it")
        tr?.printStackTrace()
        if (details.isNotEmpty()) {
            println("Details:\n    ${details.joinToString(separator = "\n    ")}")
        }
        error(it, tr, *details)
    }
}