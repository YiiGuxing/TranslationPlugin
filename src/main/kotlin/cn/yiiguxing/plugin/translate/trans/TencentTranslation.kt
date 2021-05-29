package cn.yiiguxing.plugin.translate.trans

import com.google.gson.annotations.SerializedName

@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
data class TencentTranslation(
    @SerializedName("query")
    var query: String = "",
    @SerializedName("Response")
    val response: TencentTranslationResponse
) : TranslationAdapter {
    val isSuccessful get() = response.error == null

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Cannot convert to Translation: errorCode=${response.error?.code}" }
        check(response.srcLanguage != null) { "Cannot convert to Translation: response.srcLanguage=null" }
        check(response.targetLanguage != null) { "Cannot convert to Translation: response.targetLanguage=null" }
        check(response.translation != null) { "Cannot convert to Translation: response.translation=null" }

        val srcLang = Lang.valueOfTencentCode(response.srcLanguage)
        val transLang = Lang.valueOfTencentCode(response.targetLanguage)

        return Translation(query, response.translation, srcLang, transLang, listOf(srcLang))
    }
}

data class TencentTranslationResponse(
    @SerializedName("RequestId")
    val requestId: String,

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