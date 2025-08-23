package cn.yiiguxing.plugin.translate.documentation.utils

import cn.yiiguxing.plugin.translate.documentation.Documentations
import cn.yiiguxing.plugin.translate.documentation.translateDocumentation
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
): Pair<String, Boolean> {
    var hasError = false
    val document: Document = Documentations.parseDocumentation(text)
    val translatedDocument = try {
        translator.translateDocumentation(document, language)
    } catch (e: Throwable) {
        hasError = true
        val message = getTranslationErrorMessage(e)
        Documentations.addMessage(
            Documentations.setTranslated(document, true),
            message,
            JBUI.CurrentTheme.NotificationError.foregroundColor(),
            "AllIcons.General.Error"
        )
    }

    return Documentations.getDocumentationString(translatedDocument) to hasError
}