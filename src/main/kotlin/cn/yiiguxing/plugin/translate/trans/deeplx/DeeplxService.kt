package cn.yiiguxing.plugin.translate.trans.deeplx

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.Http

/**
 * Service for the DeepL API.
 *
 * @property authKey DeepL Authentication Key as found in your [DeepL account](https://www.deepl.com/pro-account/).
 */
class DeeplxService(private var apiEndpoint: String) {

    fun translate(text: String, sourceLang: Lang, targetLang: Lang, isDocument: Boolean): String {
        val params: LinkedHashMap<String, String> = linkedMapOf(
            "target_lang" to targetLang.deeplxLanguageCodeForTarget,
            "text" to text
        )
        if (sourceLang !== Lang.AUTO) {
            params["source_lang"] = sourceLang.deeplxLanguageCodeForSource
        }
        if (isDocument) {
            params["tag_handling"] = "html"
        }

        return Http.postJson(apiEndpoint, params)
    }
}