package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.AbstractTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.MICROSOFT
import javax.swing.Icon

/**
 * Microsoft translator.
 */
object MicrosoftTranslator : AbstractTranslator() {

    override val id: String = MICROSOFT.id
    override val name: String = MICROSOFT.translatorName
    override val icon: Icon = MICROSOFT.icon
    override val intervalLimit: Int = MICROSOFT.intervalLimit
    override val contentLengthLimit: Int = MICROSOFT.contentLengthLimit
    override val primaryLanguage: Lang get() = MICROSOFT.primaryLanguage
    override val supportedSourceLanguages: List<Lang> = MicrosoftLanguageAdapter.supportedSourceLanguages
    override val supportedTargetLanguages: List<Lang> = MicrosoftLanguageAdapter.supportedTargetLanguages

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return Translation(text, text, srcLang, targetLang, emptyList())
    }
}