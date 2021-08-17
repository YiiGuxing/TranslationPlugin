package cn.yiiguxing.plugin.translate.trans.baidu

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationAdapter
import com.google.gson.annotations.SerializedName

@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
data class BaiduTranslation(
    @SerializedName("error_code")
    val code: Int = 0,
    @SerializedName("from")
    val srcLanguage: String? = null,
    @SerializedName("to")
    val targetLanguage: String? = null,
    @SerializedName("trans_result")
    val trans: List<BTrans> = emptyList()
) : TranslationAdapter {

    val isSuccessful get() = code == 0 || code == 52000

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Cannot convert to Translation: errorCode=$code" }
        check(srcLanguage != null) { "Cannot convert to Translation: srcLanguage=null" }
        check(targetLanguage != null) { "Cannot convert to Translation: targetLanguage=null" }
        check(trans.isNotEmpty()) { "Cannot convert to Translation: trans=[]" }

        val srcLang = Lang.fromBaiduLanguageCode(srcLanguage)
        val transLang = Lang.fromBaiduLanguageCode(targetLanguage)
        val original = StringBuilder()
        val translation = StringBuilder()

        trans.forEachIndexed { index, (src, dst) ->
            if (index > 0) {
                original.append('\n')
                translation.append('\n')
            }
            original.append(src)
            translation.append(dst)
        }

        return Translation(original.toString(), translation.toString(), srcLang, transLang, listOf(srcLang))
    }
}

data class BTrans(
    @SerializedName("src")
    val src: String,
    @SerializedName("dst")
    val dst: String
)
