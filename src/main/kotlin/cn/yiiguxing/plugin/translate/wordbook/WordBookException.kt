@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.wordbook

class WordBookException(
    /** @see org.sqlite.SQLiteErrorCode.code */
    val errorCode: Int,
    /** @see org.sqlite.SQLiteErrorCode.name */
    val reason: String,
    message: String,
    cause: Throwable? = null
) : Exception("[$reason] $message", cause)