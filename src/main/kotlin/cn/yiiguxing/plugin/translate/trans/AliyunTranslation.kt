package cn.yiiguxing.plugin.translate.trans

import com.google.gson.annotations.SerializedName

@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
data class AliyunTranslation(
    var query: String = "",
    var src: Lang,
    var target: Lang,
    @SerializedName("RequestId")
    val requestId: String,
    @SerializedName("Code")
    val code: Int = 0,
    @SerializedName("Data")
    val data: AliyunTranslationResponse,
    @SerializedName("Message")
    val errorMessage: String,
) : TranslationAdapter {
    val isSuccessful get() = code == 200

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Cannot convert to Translation: errorCode=${code}" }
        check(data.translation.isNotEmpty()) { "Cannot convert to Translation: data.translation = null" }

        return Translation(query, data.translation, src, target, listOf(target))
    }
}

data class AliyunTranslationResponse(

    @SerializedName("WordCount")
    val wordCount: Int? = 0,

    @SerializedName("Translated")
    val translation: String
)
