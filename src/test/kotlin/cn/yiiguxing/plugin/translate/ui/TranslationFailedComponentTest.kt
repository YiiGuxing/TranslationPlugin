package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.ErrorInfo
import cn.yiiguxing.plugin.translate.trans.TranslateException
import cn.yiiguxing.plugin.translate.trans.ali.AliTranslator

fun main() = uiTest("Translation Failed Component Test", 400, 300/*, true*/) {
    val errorInfo = ErrorInfo("错误信息摘要，错误信息摘要，错误信息摘要，错误信息摘要，错误信息摘要，错误信息摘要，错误信息摘要")
    val error = AliTranslator.let { TranslateException(it.id, it.name, errorInfo) }
    TranslationFailedComponent().apply { update(error) }
}