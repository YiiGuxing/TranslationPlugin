package cn.yiiguxing.plugin.translate.trans

import java.util.*
import javax.swing.Icon

/**
 * Translator
 *
 * Created by Yii.Guxing on 2017-10-29 0029.
 */
interface Translator {

    val id: String

    val name: String

    val icon: Icon

    val primaryLanguage: Lang

    val supportedSourceLanguages: List<Lang>

    val supportedTargetLanguages: List<Lang>

    fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation

    val defaultLangForLocale: Lang
        get() = when (Locale.getDefault().language) {
            Locale.CHINESE.language -> {
                when (Locale.getDefault().country) {
                    "HK", "TW" -> Lang.CHINESE_TRADITIONAL
                    else -> Lang.CHINESE
                }.takeIf { it in supportedTargetLanguages }
            }
            else -> {
                val language = Locale.getDefault().language
                supportedTargetLanguages.find { it.code.equals(language, ignoreCase = true) }
            }
        } ?: Lang.ENGLISH

}