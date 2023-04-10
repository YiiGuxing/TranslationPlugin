package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.AbstractTranslator
import cn.yiiguxing.plugin.translate.trans.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.OPEN_AI
import org.jsoup.nodes.Document
import javax.swing.Icon

object OpenAITranslator : AbstractTranslator(), DocumentationTranslator {

    override val id: String = OPEN_AI.id
    override val name: String = OPEN_AI.name
    override val icon: Icon = OPEN_AI.icon
    override val intervalLimit: Int = OPEN_AI.intervalLimit
    override val contentLengthLimit: Int = OPEN_AI.contentLengthLimit
    override val primaryLanguage: Lang get() = OPEN_AI.primaryLanguage
    override val supportedSourceLanguages: List<Lang> =
        OpenAILanguages.languages.toMutableList().apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = OpenAILanguages.languages

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        // TODO Call OpenAI API for translation.
        return Translation(text, text, srcLang, targetLang)
    }

    override fun translateDocumentation(documentation: Document, srcLang: Lang, targetLang: Lang): Document {
        // TODO Call OpenAI API for translation.
        return documentation
    }
}