package cn.yiiguxing.plugin.translate.trans.ali

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationAdapter
import com.google.gson.annotations.SerializedName

@Suppress("MemberVisibilityCanBePrivate")
data class AliTranslation(
    var query: String = "",
    var src: Lang,
    var target: Lang,
    @SerializedName("RequestId")
    val requestId: String,
    @SerializedName("Code")
    val code: String = "-1",
    @SerializedName("Data")
    val data: AliTranslationData,
    @SerializedName("Message")
    val errorMessage: String? = null,
) : TranslationAdapter {
    val isSuccessful get() = code == "200"

    val intCode get() = code.toIntOrNull() ?: -1

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Cannot convert to Translation: errorCode=${code}" }

        val translation = data.translation.takeIf { it.isNotEmpty() } ?: query
        return Translation(query, translation, src, target, listOf(src))
    }
}

data class AliTranslationData(
    @SerializedName("WordCount")
    val wordCount: Int? = 0,
    @SerializedName("Translated")
    val translation: String
)
