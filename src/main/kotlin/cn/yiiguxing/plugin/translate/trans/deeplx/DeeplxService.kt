package cn.yiiguxing.plugin.translate.trans.deeplx

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.Http
import com.google.gson.annotations.SerializedName
import com.intellij.util.io.RequestBuilder

/**
 * Service for the DeepLx API.
 *
 * @property authKey DeepLx Authentication Key as found in your [DeepLx account](https://www.deeplx.com/pro-account/).
 */
class DeeplxService(private var apiEndpoint: String) {

    /**
     * Translate specified texts from source language into target language.
     */
    fun translate(text: String, sourceLang: Lang, targetLang: Lang, isDocument: Boolean): String {
        val params: LinkedHashMap<String, String> = linkedMapOf(
            "target_lang" to targetLang.deeplxLanguageCode,
            "text" to text
        )
        if (sourceLang !== Lang.AUTO) {
            params["source_lang"] = sourceLang.deeplxLanguageCode
        }
        if (isDocument) {
            params["tag_handling"] = "html"
        }

        return Http.postJson(apiEndpoint, params)
    }
}
