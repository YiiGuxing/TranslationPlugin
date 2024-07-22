package cn.yiiguxing.plugin.translate.trans.ali

import cn.yiiguxing.plugin.translate.trans.TranslationResultException

class AliTranslationResultException(val errorCode: String, val errorMessage: String?) :
    TranslationResultException(errorCode.toIntOrNull() ?: -1) {
    override fun getLocalizedMessage(): String {
        return "$message[$errorMessage]"
    }
}