/*
 * YoudaoResults
 * 
 * Created by Yii.Guxing on 2017/10/30
 */
@file:Suppress("ArrayInDataClass", "MemberVisibilityCanPrivate")

package cn.yiiguxing.plugin.translate.trans

import com.google.gson.annotations.SerializedName


data class YoudaoResult(
        @SerializedName("query")
        var query: String? = null,
        @SerializedName("errorCode")
        var errorCode: Int = -1,
        var message: String? = null,
        @SerializedName("translation")
        var translation: Array<String>? = null,
        @SerializedName("basic")
        var basicExplain: BasicExplain? = null,
        @SerializedName("l")
        var languages: String? = null,
        @SerializedName("web")
        var webExplains: Array<WebExplain>? = null
) : TranslationAdapter {

    val isSuccessful get() = errorCode == 0

    fun checkError() {
        if (errorCode == 0 && translation?.isEmpty() != false && basicExplain?.explains?.isEmpty() != false) {
            errorCode = 302
        }
    }

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Can not convert to Translation: errorCode=$errorCode" }
        check(query != null) { "Can not convert to Translation: query=$query" }
        check(!languages.isNullOrBlank()) { "Can not convert to Translation: languages=$languages" }

        val languagesList = languages!!.split("2")
        check(languagesList.size == 2) { "Can not convert to Translation: languages=$languages" }

        val srcLang = Lang.fromCode(languagesList[0])
        val transLang = Lang.fromCode(languagesList[1])

        val otherExplains: Map<String, String> = webExplains?.mapNotNull { (key, values) ->
            if (key == null || values == null) {
                null
            } else {
                key to values.joinToString(separator = "; ")
            }
        }?.let {
            mapOf(*it.toTypedArray())
        } ?: emptyMap()

        return Translation(
                query!!,
                translation?.firstOrNull(),
                srcLang,
                transLang,
                basicExplain?.phonetic,
                basicExplains = basicExplain?.explains?.asList() ?: emptyList(),
                otherExplains = otherExplains)
    }
}

data class BasicExplain(
        @SerializedName(value = "phonetic")
        var phonetic: String? = null,
        @SerializedName(value = "uk-phonetic")
        var phoneticUK: String? = null,
        @SerializedName(value = "us-phonetic")
        var phoneticUS: String? = null,
        @SerializedName(value = "explains")
        var explains: Array<String>? = null
)

data class WebExplain(
        @SerializedName(value = "key")
        var key: String? = null,
        @SerializedName(value = "value")
        var values: Array<String>? = null
)