package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.documentation.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TextTranslator
import cn.yiiguxing.plugin.translate.trans.Translation
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope
import org.jsoup.nodes.Document

@Service(Service.Level.APP)
internal class MicrosoftTranslationService(
    scope: CoroutineScope
) : TextTranslator, DocumentationTranslator {

    private val bingTranslator: TextTranslator = BingTranslator(scope)
    private val documentationTranslator: DocumentationTranslator = EdgeDocumentationTranslator()

    override fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return bingTranslator.translate(text, srcLang, targetLang)
    }

    override fun translateDocumentation(documentation: Document, srcLang: Lang, targetLang: Lang): Document {
        return documentationTranslator.translateDocumentation(documentation, srcLang, targetLang)
    }
}
