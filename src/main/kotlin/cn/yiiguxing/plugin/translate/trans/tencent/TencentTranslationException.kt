package cn.yiiguxing.plugin.translate.trans.tencent

/**
 * Tencent translation exception
 */
class TencentTranslationException(
    val errorCode: String,
    val errorMessage: String
) : Exception("[$errorCode] $errorMessage")