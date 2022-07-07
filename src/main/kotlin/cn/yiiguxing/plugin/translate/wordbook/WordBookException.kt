@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.wordbook

class WordBookException(
    val errorCode: WordBookErrorCode,
    message: String,
    cause: Throwable? = null
) : Exception("$errorCode $message", cause)