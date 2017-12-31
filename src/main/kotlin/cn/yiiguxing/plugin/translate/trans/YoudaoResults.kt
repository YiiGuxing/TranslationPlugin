/*
 * YoudaoResults
 * 
 * Created by Yii.Guxing on 2017/10/30
 */
@file:Suppress("ArrayInDataClass")

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
    fun checkError() {
        if (errorCode == 0 && translation?.isEmpty() != false && basicExplain?.explains?.isEmpty() != false) {
            errorCode = 302
        }
    }

    override fun toTranslation(): Translation {
        val dictionaries = listOf(
                Dict("动词", entries = listOf(
                        DictEntry("显示", listOf("display", "show", "demonstrate", "illustrate")),
                        DictEntry("陈列", listOf("display", "exhibit", "set out")),
                        DictEntry("展出", listOf("display", "exhibit", "be on show")),
                        DictEntry("展览", listOf("exhibit", "display")),
                        DictEntry("display", listOf("显示", "陈列", "展出", "展览")),
                        DictEntry("表现",
                                listOf("show", "express", "behave", "display", "represent", "manifest")),
                        DictEntry("陈设", listOf("display", "furnish", "set out")),
                        DictEntry("陈设2", listOf("display", "furnish", "set out"))
                )),
                Dict("名词", entries = listOf(
                        DictEntry("显示", listOf("display")),
                        DictEntry("表现", listOf("performance", "show", "expression", "manifestation",
                                "representation", "display")),
                        DictEntry("炫耀", listOf("display")),
                        DictEntry("橱窗", listOf("showcase", "show window", "display", "shopwindow",
                                "glass-fronted billboard")),
                        DictEntry("罗", listOf("silk", "net", "display", "shift"))
                ))
        )

        return Translation(
                "If baby only wanted to, he could fly up to heaven this moment. It is not for nothing that he does not leave us.",
                "显示",
                Lang.ENGLISH,
                Lang.CHINESE,
                Symbol("dɪ'spleɪ", "xiǎn shì"),
                dictionaries
        )
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