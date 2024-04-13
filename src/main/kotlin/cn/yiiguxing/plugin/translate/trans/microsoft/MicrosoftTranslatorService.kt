package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.microsoft.models.MicrosoftSourceText
import cn.yiiguxing.plugin.translate.trans.microsoft.models.TextType
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread


/**
 * Service for the Microsoft Translator API.
 */
internal object MicrosoftTranslatorService {

    private const val TRANSLATE_URL = "https://api.cognitive.microsofttranslator.com/translate"

    @RequiresBackgroundThread
    fun translate(text: String, from: Lang, to: Lang, textType: TextType = TextType.PLAIN): String {
        val translateUrl = UrlBuilder(TRANSLATE_URL)
            .addQueryParameter("api-version", "3.0")
            .apply { if (from != Lang.AUTO) addQueryParameter("from", from.microsoftLanguageCode) }
            .addQueryParameter("to", to.microsoftLanguageCode)
            .addQueryParameter("textType", textType.value)
            .build()

        val accessToken = MicrosoftEdgeAuthService.service().getAccessToken()
        return MicrosoftHttp.post(translateUrl, accessToken, listOf(MicrosoftSourceText(text)))
    }
}
