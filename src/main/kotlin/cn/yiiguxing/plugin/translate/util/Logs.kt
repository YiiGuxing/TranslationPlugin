/*
 * Logs
 */
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.diagnostic.Logger


fun Logger.d(message: String) {
    println("DEBUG - $message")
    debug(message)
}

fun Logger.i(message: String) {
    println("INFO - $message")
    info(message)
}

fun Logger.w(tr: Throwable) {
    w(tr.message.toString(), tr)
}

fun Logger.w(message: String, tr: Throwable? = null) {
    println("WARN - $message")
    tr?.printStackTrace()
    warn(message, tr)
}

fun Logger.e(message: String, tr: Throwable? = null, vararg details: String) {
    println("ERROR - $message")
    tr?.printStackTrace()
    if (details.isNotEmpty()) {
        println("Details:\n    ${details.joinToString(separator = "\n    ")}")
    }
    error(message, tr, *details)
}