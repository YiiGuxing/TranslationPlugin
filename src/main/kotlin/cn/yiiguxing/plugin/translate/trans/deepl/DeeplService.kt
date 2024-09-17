package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.Http
import com.google.gson.annotations.SerializedName
import com.intellij.util.io.RequestBuilder

/**
 * Service for the DeepL API.
 *
 * @property authKey DeepL Authentication Key as found in your [DeepL account](https://www.deepl.com/pro-account/).
 */
class DeeplService(var authKey: String) {

    /**
     * Determines if the given [DeepL Authentication Key][authKey] belongs to an API-Free account.
     */
    val isFreeAccount: Boolean get() = isFreeAccountAuthKey(authKey)

    /** Base URL for DeepL API.  */
    private val serverUrl: String
        get() = if (isFreeAccount) DEEPL_SERVER_URL_FREE else DEEPL_SERVER_URL_PRO

    /**
     * Sets the Authorization and the User Agent HTTP header.
     */
    private fun RequestBuilder.auth() {
        userAgent(Http.PLUGIN_USER_AGENT)
        // Authentication method should be header-based authentication,
        // the auth-key will leak into the log file if it is authenticated as a parameter.
        tuner { it.setRequestProperty("Authorization", "DeepL-Auth-Key $authKey") }
    }

    /**
     * Translate specified texts from source language into target language.
     */
    fun translate(text: String, sourceLang: Lang, targetLang: Lang, isDocument: Boolean): String {
        val params: LinkedHashMap<String, String> = linkedMapOf(
            "target_lang" to targetLang.deeplLanguageCodeForTarget,
            "text" to text
        )
        if (sourceLang !== Lang.AUTO) {
            params["source_lang"] = sourceLang.deeplLanguageCodeForSource
        }
        if (isDocument) {
            params["tag_handling"] = "html"
        }

        return Http.post("$serverUrl/v2/translate", params) { auth() }
    }

    /**
     * Retrieves the usage in the current billing period for this DeepL account.
     *
     * @return [Usage] object containing account usage information.
     */
    fun getUsage(): Usage = Http.request("$serverUrl/v2/usage") { auth() }

    /**
     * Information about DeepL account usage for the current billing period, for example, the number of
     * characters translated.
     *
     * Depending on the account type, some usage types will be omitted. See the
     * [API documentation](https://www.deepl.com/docs-api/general/get-usage/) for more information.
     */
    data class Usage(
        @SerializedName("character_count")
        val characterCount: Int,
        @SerializedName("character_limit")
        val characterLimit: Int
    ) {
        val limitReached: Boolean get() = characterCount >= characterLimit
    }

    companion object {
        /** Base URL for DeepL API Free accounts. */
        private const val DEEPL_SERVER_URL_FREE = "https://api-free.deepl.com"

        /** Base URL for DeepL API Pro accounts */
        private const val DEEPL_SERVER_URL_PRO = "https://api.deepl.com"

        /**
         * Determines if the given [DeepL Authentication Key][authKey] belongs to an API-Free account.
         *
         * @param authKey DeepL Authentication Key as found in your [DeepL account](https://www.deepl.com/pro-account/).
         * @return `true` if the Authentication Key belongs to an API-Free account, otherwise `false`.
         */
        fun isFreeAccountAuthKey(authKey: String): Boolean = authKey.endsWith(":fx")
    }
}