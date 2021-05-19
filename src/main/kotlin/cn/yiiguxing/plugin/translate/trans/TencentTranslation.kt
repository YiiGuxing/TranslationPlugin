package cn.yiiguxing.plugin.translate.trans

import com.google.gson.annotations.SerializedName

@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
data class TencentTranslation(
    @SerializedName("Response")
    var response: Response = Response(),

    @SerializedName("query")
    var query: String? = null
) : TranslationAdapter {
    val isSuccessful get() = response.error == null

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Cannot convert to Translation: errorCode=${response.error?.code}" }
        check(response.srcLanguage != null) { "Cannot convert to Translation: srcLanguage=null" }
        check(response.targetLanguage != null) { "Cannot convert to Translation: targetLanguage=null" }
        check(response.translation != null) { "Cannot convert to Translation: trans=null" }

        val srcLang = Lang.valueOfBaiduCode(response.srcLanguage!!)
        val transLang = Lang.valueOfBaiduCode(response.targetLanguage!!)

        return Translation(query!!, response.translation.toString(), srcLang, transLang, listOf(srcLang))
    }
}

data class Response(
    @SerializedName("RequestId")
    val requestId: String = "",

    @SerializedName("Error")
    val error: TencentError? = null,

    @SerializedName("Source")
    val srcLanguage: String? = null,
    @SerializedName("Target")
    val targetLanguage: String? = null,
    @SerializedName("TargetText")
    val translation: String? = null
)

data class TencentError(
    @SerializedName("Code")
    val code: String,
    @SerializedName("Message")
    val message: String
)