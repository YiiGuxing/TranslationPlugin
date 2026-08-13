package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.microsoft.models.AzureAuthentication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import org.jsoup.nodes.Document

internal object MicrosoftTranslatorService : TextTranslator, DocumentationTranslator {

    private val settings get() = MicrosoftSettings.getInstance()
    private val bingService = BingEdgeTranslatorService(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    private val azureService = AzureTranslatorService {
        AzureAuthentication.SubscriptionKey(settings.getSubscriptionKey(), settings.region.takeIf { it.isNotBlank() })
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation =
        when (settings.translator) {
            MicrosoftTranslatorType.AZURE -> azureService.translate(text, srcLang, targetLang)
            MicrosoftTranslatorType.BING_EDGE -> bingService.translate(text, srcLang, targetLang)
        }

    override fun translateDocumentation(documentation: Document, srcLang: Lang, targetLang: Lang): Document =
        when (settings.translator) {
            MicrosoftTranslatorType.AZURE -> azureService.translateDocumentation(documentation, srcLang, targetLang)
            MicrosoftTranslatorType.BING_EDGE -> bingService.translateDocumentation(documentation, srcLang, targetLang)
        }
}
