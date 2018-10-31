/*
 * YoudaoTranslation
 * 
 * Created by Yii.Guxing on 2017/10/30
 */
@file:Suppress("ArrayInDataClass", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.util.Settings
import com.google.gson.annotations.SerializedName


data class YoudaoTranslation(
        @SerializedName("query")
        var query: String? = null,
        @SerializedName("errorCode")
        var errorCode: Int = -1,
        var message: String? = null,
        @SerializedName("translation")
        var translation: Array<String>? = null,
        @SerializedName("basic")
        var basicExplain: YBasicExplain? = null,
        @SerializedName("l")
        var languages: String? = null,
        @SerializedName("web")
        var webExplains: Array<YWebExplain>? = null
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

        val srcLang = Lang.valueOfYoudaoCode(languagesList[0])
        val transLang = Lang.valueOfYoudaoCode(languagesList[1])

        val otherExplains: Map<String, String> = webExplains?.mapNotNull { (key, values) ->
            if (key == null || values == null) {
                null
            } else {
                key to values.joinToString(separator = "; ")
            }
        }?.let {
            mapOf(*it.toTypedArray())
        } ?: emptyMap()

        val basicExplains = ArrayList<String>()
        basicExplain?.explains?.let { basicExplains.addAll(it) }
        if (Settings.showWordForms) {
            basicExplain?.wordForms?.joinToString("\n", "\n") { it.wordForm.toString() }
                    ?.let { basicExplains.add(it) }
        }

        return Translation(
                query!!,
                translation?.firstOrNull(),
                srcLang,
                transLang,
                listOf(srcLang),
                basicExplain?.phonetic,
                basicExplains = basicExplains,
                otherExplains = otherExplains)
    }
}

data class YBasicExplain(
        @SerializedName(value = "phonetic")
        var phonetic: String? = null,
        @SerializedName(value = "uk-phonetic")
        var phoneticUK: String? = null,
        @SerializedName(value = "us-phonetic")
        var phoneticUS: String? = null,
        @SerializedName(value = "explains")
        var explains: Array<String>? = null,
        @SerializedName(value = "wfs")
        var wordForms: Array<YWordFormWrapper>? = null)

data class YWebExplain(
        @SerializedName(value = "key")
        var key: String? = null,
        @SerializedName(value = "value")
        var values: Array<String>? = null)

data class YWordFormWrapper(@SerializedName(value = "wf") val wordForm: YWordForm)
data class YWordForm(
        @SerializedName(value = "name")
        val name: String,
        @SerializedName(value = "value")
        val value: String
) {
    override fun toString(): String {
        return "$name: ${value.replace("\\s*或\\s*".toRegex(), ", ")}"
    }
}