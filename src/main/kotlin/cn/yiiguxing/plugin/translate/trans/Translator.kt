package cn.yiiguxing.plugin.translate.trans

import javax.swing.Icon

/**
 * Translator
 */
interface Translator {

    val id: String

    val name: String

    val icon: Icon

    val primaryLanguage: Lang

    val supportedSourceLanguages: List<Lang>

    val supportedTargetLanguages: List<Lang>

    val intervalLimit: Int

    val contentLengthLimit: Int

    fun checkConfiguration(force: Boolean = false): Boolean = true

    fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation

    val defaultLangForLocale: Lang

}