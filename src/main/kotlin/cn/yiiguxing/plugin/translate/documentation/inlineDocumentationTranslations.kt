package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.trans.getTranslationErrorMessage
import com.intellij.lang.Language
import com.intellij.util.ui.JBUI
import org.jsoup.nodes.Document


internal fun translateInlineDocumentation(
    text: String,
    language: Language,
    translator: Translator = TranslateService.getInstance().translator
): InlineDocTranslationInfo {
    var hasError = false
    val document: Document = Documentations.parseDocumentation(text)
    try {
        translator.translateDocumentation(document, language)
    } catch (e: Throwable) {
        hasError = true
        val message = getTranslationErrorMessage(e)
        Documentations.addMessage(
            Documentations.setTranslated(document, translator.id),
            message,
            JBUI.CurrentTheme.NotificationError.foregroundColor(),
            "AllIcons.General.Error"
        )
    }

    val translatedText = Documentations.getDocumentationString(document)
    return InlineDocTranslationInfo.translated(translatedText, translator.id, hasError)
}